package net.kuafuai.andee.brain

import net.kuafuai.andee.brain.provider.AiProvider
import net.kuafuai.andee.brain.provider.AiProviders
import net.kuafuai.andee.brain.provider.AnthropicWire
import net.kuafuai.andee.brain.provider.GeminiWire
import net.kuafuai.andee.brain.provider.LlmErrors
import net.kuafuai.andee.brain.provider.LlmErrorKind
import net.kuafuai.andee.brain.provider.OpenAiWire
import net.kuafuai.andee.brain.provider.WireFormat
import net.kuafuai.andee.brain.provider.WireReply
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A non-2xx answer from the backend. A type of its own so the status code
 * survives to LocalBrain, which has a Context and can turn it into a
 * sentence the user can act on; this class has none and must not grow one.
 * The message stays "LLM HTTP <code>: <body>" so logs read as they always did.
 */
/**
 * The backend address answered with something other than the API: a redirect, or a page that is not JSON. The
 * address is wrong or the service moved; [where] is where it pointed, [sample] what came back instead.
 */
class LlmMovedException(val code: Int, val where: String, val sample: String = "") :
    IllegalStateException(
        "the backend did not answer as an API (HTTP " + code +
            (if (where.isNotEmpty()) " -> " + where else "") +
            (if (sample.isNotEmpty()) ": " + sample else "") + ")",
    )

class LlmHttpException(val code: Int, val body: String) :
    IllegalStateException("LLM HTTP " + code + ": " + body.take(600)) {

    /**
     * The gateway's own words for what it disliked, without the JSON around
     * them. Falls back to the raw body for gateways that don't follow the
     * {"error":{"message":...}} shape.
     */
    val detail: String
        get() = runCatching {
            val err = JSONObject(body).opt("error")
            when (err) {
                is JSONObject -> err.optString("message")
                is String -> err
                else -> ""
            }
        }.getOrDefault("").ifBlank { body }.trim()
}

/**
 * One blocking call to a chat-completions-style API, aimed at whichever provider the user configured.
 *
 * ### One canonical form, three wires
 *
 * The conversation the brain builds is OpenAI-shaped JSON. This class keeps that shape and hands the
 * request to one of three translators at the edge ([OpenAiWire], [GeminiWire], [AnthropicWire]). The
 * reply is translated back before anyone above sees it, so LocalBrain, the context pruner and the archive
 * never learn which provider answered — that is what makes a provider switch a settings change rather than
 * a rewrite.
 *
 * Deliberately thin and deliberately **not** streaming. Streaming would let TTS start talking a second or
 * two earlier, but it costs an SSE parser plus incremental tool_calls reassembly (arguments arrive as
 * string fragments across deltas), and none of that is needed to make the local brain work.
 *
 * Two shapes in here are easy to get wrong, and both are silent when wrong:
 *
 *  * tool_calls[].function.arguments is a JSON *string*, not an object. It has to be re-parsed, and a
 *    model that emits malformed JSON must not take the turn down — see [ToolCall.argumentsOrNull].
 *  * reasoning_content must never be sent back on the providers that reject it. [Reply.reasoning] exists
 *    to be logged and thrown away. DeepSeek is the exception: inside a tool-call turn it wants the
 *    reasoning it produced echoed back, and [sanitizeAssistant] is the only place that decides.
 *
 * ### Retry and fallback
 *
 * A rate limit or a server hiccup is retried on the same endpoint ([maxRetries] times, capped backoff); a
 * failure the fallback provider can route around (auth, quota, network, 5xx) is tried once on
 * [fallback]. A 400 is returned immediately: the request is malformed, and buying the same error from a
 * second provider only spends the user's money. Neither path loops: retries are bounded and the fallback
 * has no fallback of its own.
 *
 * Threading: [complete] blocks the calling thread. It is only ever called from [LocalBrain]'s single loop
 * thread, and from settings' "test connection" on a worker.
 */
class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
    model: String,
    thinking: Boolean,
    reasoningEffort: String,
    /**
     * How long to wait for an answer. 180 s for a turn (a thinking model answers in minutes); a caller that
     * classifies something small and runs under a deadline of its own — the notification triage, inside the
     * phone's wake lock — passes less.
     */
    readTimeoutSeconds: Long = 180,
    /** Which provider this endpoint belongs to. Blank means "work it out from the URL" (older settings). */
    providerId: String = "",
    private val temperature: Double? = null,
    private val topP: Double? = null,
    private val maxOutputTokens: Int? = null,
    private val extraHeaders: Map<String, String> = emptyMap(),
    /** Retries on the same endpoint for transient failures (429, 5xx, network). 0 disables retrying. */
    private val maxRetries: Int = 1,
    /** Tried once when the primary endpoint fails in a way a different provider can route around. */
    private val fallback: LlmClient? = null,
) {

    /** The provider this endpoint speaks to. Resolved once; the URL does not change for a client's life. */
    val provider: AiProvider = AiProviders.providerFor(providerId, baseUrl)
    val wire: WireFormat get() = provider.wire

    // Request parameters, read once per complete(). Volatile because retune
    // lands from the main thread while the loop thread may be mid-turn; the turn
    // in flight keeps what it already sent and the next request carries the new.
    @Volatile private var model = model
    @Volatile private var thinking = thinking
    @Volatile private var reasoningEffort = reasoningEffort

    /**
     * Change what the next request asks for, without touching the endpoint or
     * the key. Those two are the identity of the backend; these three are only
     * how it is asked, so a settings change to them has no reason to cost the
     * user their conversation.
     */
    fun retune(model: String, thinking: Boolean, reasoningEffort: String) {
        this.model = model
        this.thinking = thinking
        this.reasoningEffort = reasoningEffort
    }

    /**
     * Read timeout is generous because a thinking model answers in minutes, not
     * seconds, and a premature timeout here reads to the user as the ball going
     * quiet forever. Connect timeout stays short: a tablet that has lost its
     * network should say so quickly.
     */
    private val http = OkHttpClient.Builder()
        // Never follow a redirect. OkHttp turns a POST that meets a 301 into a GET of the new address, which on a
        // moved gateway is a web page: the turn then died on "A JSONObject text must begin with '{'", which tells
        // nobody anything (measured 2026-10-28: codeflying.net -> www.codeflying.net, an nginx page). A redirect
        // here is an address that is wrong or has moved, and it is reported as that — see [LlmMovedException].
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** The in-flight call, so [cancel] can cut a turn short. */
    @Volatile
    private var current: okhttp3.Call? = null

    data class ToolCall(
        val id: String,
        val name: String,
        /** Raw JSON text as the model wrote it. */
        val argumentsRaw: String,
    ) {
        /**
         * Null when the model wrote something that isn't a JSON object. The
         * caller turns that into a tool-error message rather than an exception:
         * a model that miswrote its arguments can fix them on the next
         * iteration, and killing the turn denies it the chance.
         */
        fun argumentsOrNull(): JSONObject? =
            if (argumentsRaw.isBlank()) JSONObject()
            else runCatching { JSONObject(argumentsRaw) }.getOrNull()

        /** Identity for the repeated-call guard in LocalBrain. */
        fun signature(): String = name + "/" + argumentsRaw
    }

    data class Reply(
        /** Assistant prose. Narration when [toolCalls] is non-empty, otherwise the answer. */
        val content: String,
        /** Logged only — see the class KDoc. */
        val reasoning: String,
        val toolCalls: List<ToolCall>,
        /**
         * The assistant message exactly as it must be echoed back into the
         * history. Kept verbatim from the response rather than rebuilt, minus
         * the fields the provider rejects on the way back, so no field the API
         * cares about gets lost in translation.
         */
        val assistantMessage: JSONObject,
        val promptTokens: Int,
        val completionTokens: Int,
        val cachedTokens: Int,
    )

    /**
     * DeepSeek's own API, and the Volcengine/Ark gateways this app is commonly pointed at, accept the
     * thinking switch. The same field on api.openai.com is an unknown argument and a 400, so it is sent
     * only where it is understood.
     */
    private val arkThinking: Boolean
        get() {
            val u = baseUrl.lowercase()
            return provider.id == AiProviders.DEEPSEEK || "volces" in u || "codeflying" in u || "ark." in u
        }

    internal fun buildRequestBody(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean = false): JSONObject =
        OpenAiWire.body(
            providerId = provider.id,
            model = model,
            messages = messages,
            tools = tools,
            thinking = thinking && !noThinking,
            reasoningEffort = reasoningEffort,
            temperature = temperature,
            topP = topP,
            maxOutputTokens = maxOutputTokens,
            arkThinking = arkThinking,
        )

    /**
     * @param messages the full conversation, system message first.
     * @param tools OpenAI-format function specs — see [toolSpecs].
     * @param noThinking ask without reasoning whatever the user's setting is.
     */
    fun complete(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean = false): Reply {
        var attempt = 0
        while (true) {
            try {
                return completeOnce(messages, tools, noThinking)
            } catch (e: LlmMovedException) {
                throw e
            } catch (e: LlmHttpException) {
                val kind = LlmErrors.classify(e.code)
                if (LlmErrors.retryable(kind) && attempt < maxRetries) {
                    attempt++
                    backoff(attempt)
                    continue
                }
                if (fallback != null && LlmErrors.fallbackEligible(kind)) return tryFallback(messages, tools, noThinking)
                throw e
            } catch (e: IOException) {
                if (attempt < maxRetries) {
                    attempt++
                    backoff(attempt)
                    continue
                }
                if (fallback != null) return tryFallback(messages, tools, noThinking)
                throw e
            }
        }
    }

    /** One attempt on the fallback endpoint. The fallback's own failure is the one the user should see. */
    private fun tryFallback(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean): Reply {
        val fb = fallback ?: throw IllegalStateException("no fallback")
        return fb.complete(messages, tools, noThinking)
    }

    private fun backoff(attempt: Int) {
        // 400 ms, 1,6 s, capped: a rate limit that needs a minute is better served by the fallback than by
        // holding the user's ball silent.
        val ms = minOf(400L shl (attempt - 1).coerceAtMost(2), 1_600L)
        try {
            Thread.sleep(ms)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun completeOnce(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean): Reply {
        val request = when (wire) {
            WireFormat.OPENAI -> openAiRequest(messages, tools, noThinking)
            WireFormat.GEMINI -> geminiRequest(messages, tools, noThinking)
            WireFormat.ANTHROPIC -> anthropicRequest(messages, tools, noThinking)
        }
        val text = execute(request)
        return when (wire) {
            WireFormat.OPENAI -> parseOpenAi(text)
            WireFormat.GEMINI -> parseWire(text) { GeminiWire.parse(it) }
            WireFormat.ANTHROPIC -> parseWire(text) { AnthropicWire.parse(it) }
        }
    }

    private fun openAiRequest(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean): Request {
        val body = buildRequestBody(messages, tools, noThinking)
        val headers = OpenAiWire.headers(apiKey, extraHeaders)
        return requestBuilder(OpenAiWire.endpoint(baseUrl))
            .apply { for ((k, v) in headers) addHeader(k, v) }
            .post(body.toString().toRequestBody(JSON))
            .build()
    }

    private fun geminiRequest(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean): Request {
        val body = GeminiWire.body(
            model = model,
            messages = messages,
            tools = tools,
            thinking = thinking && !noThinking,
            reasoningEffort = reasoningEffort,
            temperature = temperature,
            topP = topP,
            maxOutputTokens = maxOutputTokens,
        )
        val headers = GeminiWire.headers(apiKey, extraHeaders)
        return requestBuilder(GeminiWire.endpoint(baseUrl, model))
            .apply { for ((k, v) in headers) addHeader(k, v) }
            .post(body.toString().toRequestBody(JSON))
            .build()
    }

    private fun anthropicRequest(messages: List<JSONObject>, tools: JSONArray, noThinking: Boolean): Request {
        val body = AnthropicWire.body(
            model = model,
            messages = messages,
            tools = tools,
            thinking = thinking && !noThinking,
            reasoningEffort = reasoningEffort,
            temperature = temperature,
            topP = topP,
            maxOutputTokens = maxOutputTokens,
        )
        val headers = AnthropicWire.headers(apiKey, extraHeaders)
        return requestBuilder(AnthropicWire.endpoint(baseUrl))
            .apply { for ((k, v) in headers) addHeader(k, v) }
            .post(body.toString().toRequestBody(JSON))
            .build()
    }

    /** No headers here on purpose: each wire adds its own, and an OpenAI Bearer header on a Gemini or
     *  Anthropic request would put the key somewhere the provider did not ask for it. */
    private fun requestBuilder(url: String): Request.Builder = Request.Builder().url(url)

    /** Execute, reading the body either way: the useful part of an error is in it. */
    private fun execute(req: Request): String {
        val call = http.newCall(req)
        current = call
        return try {
            call.execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (resp.code in 300..399) throw LlmMovedException(resp.code, resp.header("Location").orEmpty())
                if (!resp.isSuccessful) throw LlmHttpException(resp.code, raw)
                raw
            }
        } finally {
            current = null
        }
    }

    private fun parseOpenAi(text: String): Reply {
        val root = runCatching { JSONObject(text) }.getOrElse {
            throw LlmMovedException(200, "", text.trim().take(80).replace(Regex("\\s+"), " "))
        }
        val wr = OpenAiWire.parse(provider.id, root)
        val msg = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
        return wrap(
            wr,
            if (msg != null) sanitizeAssistant(msg) else wr.assistantMessage,
        )
    }

    private inline fun parseWire(text: String, parse: (JSONObject) -> WireReply): Reply {
        val root = runCatching { JSONObject(text) }.getOrElse {
            throw LlmMovedException(200, "", text.trim().take(80).replace(Regex("\\s+"), " "))
        }
        return wrap(parse(root), null)
    }

    private fun wrap(wr: WireReply, assistantOverride: JSONObject?): Reply = Reply(
        content = wr.content,
        reasoning = wr.reasoning,
        toolCalls = wr.toolCalls.map { ToolCall(it.id, it.name, it.argumentsRaw) },
        assistantMessage = assistantOverride ?: wr.assistantMessage,
        promptTokens = wr.promptTokens,
        completionTokens = wr.completionTokens,
        cachedTokens = wr.cachedTokens,
    )

    /**
     * Abort whatever is in flight. Called from the main thread when the user
     * hits ■, so it must not block — okhttp3.Call.cancel doesn't.
     */
    fun cancel() {
        val c = current ?: return
        runCatching { c.cancel() }
    }

    /**
     * The canonical assistant message for the history. Content is always present (JSONObject.NULL when the
     * turn was only tool calls), and reasoning_content is passed back only where the provider wants it.
     */
    internal fun sanitizeAssistant(msg: JSONObject): JSONObject {
        val out = JSONObject()
        out.put("role", "assistant")
        val content = msg.optString("content").orEmpty()
        out.put("content", if (content.isEmpty()) JSONObject.NULL else content)
        if (isDeepSeek && msg.has("reasoning_content") && !msg.isNull("reasoning_content")) {
            out.put("reasoning_content", msg.get("reasoning_content"))
        }
        msg.optJSONArray("tool_calls")?.let { if (it.length() > 0) out.put("tool_calls", it) }
        return out
    }

    private val isDeepSeek: Boolean get() = provider.id == AiProviders.DEEPSEEK

    companion object {
        private const val TAG = "Brain"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * Turn the body's own schema list into OpenAI function specs.
         *
         * The dispatcher already emits {name, description, parameters} with OpenAI's JSON Schema dialect —
         * the only differences are the wrapper and the method field, which is this device's internal
         * routing key and means nothing to the model.
         */
        fun toolSpecs(vararg schemaLists: JSONArray): JSONArray {
            val out = JSONArray()
            for (list in schemaLists) {
                for (i in 0 until list.length()) {
                    val t = list.optJSONObject(i) ?: continue
                    val fn = JSONObject()
                        .put("name", t.optString("name"))
                        .put("description", t.optString("description"))
                        .put(
                            "parameters",
                            t.optJSONObject("parameters")
                                ?: JSONObject().put("type", "object").put("properties", JSONObject()),
                        )
                    out.put(JSONObject().put("type", "function").put("function", fn))
                }
            }
            return out
        }

        /**
         * A vision content block carrying an inline image.
         *
         * The canonical (OpenAI-compatible) form; the Gemini and Anthropic translators read this shape and
         * turn it into inlineData / base64 source. Isolated into one function so there is one place that
         * knows what an image looks like.
         */
        fun imageBlock(base64: String, mime: String = "image/png"): JSONObject =
            JSONObject()
                .put("type", "image_url")
                .put(
                    "image_url",
                    JSONObject().put("url", "data:" + mime + ";base64," + base64),
                )

        fun textBlock(text: String): JSONObject =
            JSONObject().put("type", "text").put("text", text)
    }
}
