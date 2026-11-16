package net.kuafuai.andee.brain.provider

import org.json.JSONArray
import org.json.JSONObject

/**
 * One tool call as a provider returned it, before it is made canonical.
 *
 * argumentsRaw is a JSON *string* in every provider that gives us tool calls, which is why it stays a
 * string here too: parsing it is the caller's job, and a model that writes malformed JSON must be able to
 * fail one call instead of taking the whole turn down.
 */
data class WireToolCall(val id: String, val name: String, val argumentsRaw: String)

/** A reply translated into the canonical (OpenAI-shaped) form LlmClient and LocalBrain already use. */
data class WireReply(
    val content: String,
    val reasoning: String,
    val toolCalls: List<WireToolCall>,
    val assistantMessage: JSONObject,
    val promptTokens: Int,
    val completionTokens: Int,
    val cachedTokens: Int,
)

/**
 * Translators between the canonical chat history (OpenAI message objects, the shape LocalBrain builds)
 * and each provider's own wire format.
 *
 * The canonical form is not an accident of history: the whole agent loop, the context pruner and the
 * conversation archive already speak it, so adapting at the edge of the HTTP call is the only place a
 * second format can be contained. Everything above LlmClient is provider-blind.
 *
 * Two translation rules that are easy to get wrong and silent when wrong:
 *
 *  * Tool results carry only a tool_call_id. Gemini and Anthropic both want the name of the function
 *    back, so both translators walk the history in order and remember id -> name from the assistant
 *    message that issued the call.
 *  * Gemini and Anthropic both reject two same-role messages in a row. The canonical history can
 *    contain them (an image is re-attached as a user message right after another user message), so the
 *    translators merge adjacent turns instead of forwarding them as-is.
 */
internal object Jsons {
    fun str(o: JSONObject?, key: String): String = o?.optString(key).orEmpty()

    fun arr(o: JSONObject?, key: String): JSONArray? = o?.optJSONArray(key)

    /** The text of a canonical message content field, which may be a string or an array of blocks. */
    fun contentText(content: Any?): String = when (content) {
        null, JSONObject.NULL -> ""
        is String -> content
        is JSONArray -> {
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val b = content.optJSONObject(i) ?: continue
                if (b.optString("type") == "text") sb.append(b.optString("text"))
            }
            sb.toString()
        }
        else -> content.toString()
    }

    /** Parse a model-written argument string; null when it is not a JSON object (the caller reports it). */
    fun args(raw: String): JSONObject? =
        if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }.getOrNull()

    /** Strip JSON-Schema keys Gemini's functionDeclarations do not accept. */
    fun geminiSchema(src: JSONObject?): JSONObject {
        val out = JSONObject()
        for (k in listOf("type", "description", "nullable", "format", "enum", "minimum", "maximum", "required")) {
            val v = src?.opt(k)
            if (v is JSONArray || v is String || v is Number || v is Boolean) out.put(k, v)
        }
        src?.optJSONObject("items")?.let { out.put("items", geminiSchema(it)) }
        src?.optJSONObject("properties")?.let { props ->
            val p = JSONObject()
            for (name in props.keys()) {
                val child = props.optJSONObject(name)
                p.put(name, if (child != null) geminiSchema(child) else JSONObject().put("type", "string"))
            }
            out.put("properties", p)
        }
        return out
    }
}

/** The OpenAI chat-completions wire: OpenAI, DeepSeek, OpenRouter, Ollama and every compatible gateway. */
object OpenAiWire {

    fun endpoint(baseUrl: String): String {
        // A base URL that already names the operation is used verbatim: pasting the full URL from a
        // provider's documentation should work rather than produce a doubled path.
        val b = baseUrl.trim().trimEnd('/')
        if (b.endsWith("/chat/completions")) return b
        return b + "/chat/completions"
    }

    fun headers(apiKey: String, extra: Map<String, String>): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["Content-Type"] = "application/json; charset=utf-8"
        if (apiKey.isNotBlank()) h["Authorization"] = "Bearer " + apiKey.trim()
        for ((k, v) in extra) if (k.isNotBlank() && v.isNotBlank()) h[k.trim()] = v.trim()
        return h
    }

    fun body(
        providerId: String,
        model: String,
        messages: List<JSONObject>,
        tools: JSONArray,
        thinking: Boolean,
        reasoningEffort: String,
        temperature: Double?,
        topP: Double?,
        maxOutputTokens: Int?,
        /**
         * Whether this gateway understands the "thinking" switch. DeepSeek's own API and the Ark/Volcengine
         * gateways this app is commonly proxied through do; api.openai.com treats it as an unknown argument
         * and rejects the whole request, which is why the caller decides instead of the body builder.
         */
        arkThinking: Boolean = true,
        stream: Boolean = false,
    ): JSONObject {
        val msgArr = JSONArray()
        for (m in messages) msgArr.put(m)
        val body = JSONObject()
            .put("model", model)
            .put("messages", msgArr)
            .put("stream", stream)
        if (tools.length() > 0) {
            body.put("tools", tools).put("tool_choice", "auto")
        }
        if (thinking) {
            // Two different switches for the same idea. DeepSeek names it "thinking"; the OpenAI-compatible
            // gateways name it "reasoning_effort". Each is sent only where it is understood.
            if (arkThinking) body.put("thinking", JSONObject().put("type", "enabled"))
            if (providerId != AiProviders.DEEPSEEK) body.put("reasoning_effort", reasoningEffort)
        } else {
            // Always explicit on the gateways that take it, except on a reasoner, whose whole job is to
            // think and which rejects the disabled block.
            if (arkThinking && !model.contains("reasoner", ignoreCase = true)) {
                body.put("thinking", JSONObject().put("type", "disabled"))
            }
        }
        temperature?.let { body.put("temperature", it) }
        topP?.let { body.put("top_p", it) }
        maxOutputTokens?.let { body.put("max_tokens", it) }
        return body
    }

    fun parse(providerId: String, root: JSONObject): WireReply {
        val choice = root.optJSONArray("choices")?.optJSONObject(0)
            ?: throw IllegalStateException("LLM reply has no choices: " + root.toString().take(300))
        val msg = choice.optJSONObject("message")
            ?: throw IllegalStateException("LLM choice has no message: " + root.toString().take(300))
        val calls = mutableListOf<WireToolCall>()
        msg.optJSONArray("tool_calls")?.let { arr ->
            for (i in 0 until arr.length()) {
                val tc = arr.optJSONObject(i) ?: continue
                val fn = tc.optJSONObject("function") ?: continue
                calls += WireToolCall(
                    id = tc.optString("id").ifEmpty { "call_" + i },
                    name = fn.optString("name"),
                    argumentsRaw = fn.optString("arguments"),
                )
            }
        }
        val usage = root.optJSONObject("usage")
        val details = usage?.optJSONObject("prompt_tokens_details")
        val cached = when {
            details != null && details.has("cached_tokens") -> details.optInt("cached_tokens")
            usage != null -> usage.optInt("prompt_cache_hit_tokens")
            else -> 0
        }
        return WireReply(
            content = msg.optString("content").orEmpty(),
            reasoning = msg.optString("reasoning_content").orEmpty(),
            toolCalls = calls,
            assistantMessage = canonicalAssistant(msg),
            promptTokens = usage?.optInt("prompt_tokens") ?: 0,
            completionTokens = usage?.optInt("completion_tokens") ?: 0,
            cachedTokens = cached,
        )
    }

    /**
     * The assistant message exactly as it must be echoed back. Kept verbatim: no field the API cares about
     * gets lost in translation, and reasoning_content is dropped because every provider here rejects sending
     * it back.
     */
    fun canonicalAssistant(msg: JSONObject): JSONObject {
        val out = JSONObject()
        out.put("role", "assistant")
        val content = msg.optString("content").orEmpty()
        out.put("content", if (content.isEmpty()) JSONObject.NULL else content)
        msg.optJSONArray("tool_calls")?.let { if (it.length() > 0) out.put("tool_calls", it) }
        return out
    }
}

/** Google Gemini's generateContent wire. */
object GeminiWire {

    fun endpoint(baseUrl: String, model: String): String {
        val b0 = baseUrl.trim().trimEnd('/')
        if (b0.endsWith(":generateContent")) return b0
        val b = if (b0.endsWith("/models")) b0.removeSuffix("/models") else b0
        return b + "/models/" + model.trim() + ":generateContent"
    }

    fun headers(apiKey: String, extra: Map<String, String>): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["Content-Type"] = "application/json; charset=utf-8"
        // Header, not the key query parameter: a URL ends up in logs and crash reports, and so would the key.
        if (apiKey.isNotBlank()) h["x-goog-api-key"] = apiKey.trim()
        for ((k, v) in extra) if (k.isNotBlank() && v.isNotBlank()) h[k.trim()] = v.trim()
        return h
    }

    fun body(
        model: String,
        messages: List<JSONObject>,
        tools: JSONArray,
        thinking: Boolean,
        reasoningEffort: String,
        temperature: Double?,
        topP: Double?,
        maxOutputTokens: Int?,
    ): JSONObject {
        val system = StringBuilder()
        val contents = JSONArray()
        val toolNames = HashMap<String, String>()

        fun append(role: String, parts: JSONArray) {
            if (parts.length() == 0) return
            val last = if (contents.length() > 0) contents.optJSONObject(contents.length() - 1) else null
            if (last != null && last.optString("role") == role) {
                val merged = last.optJSONArray("parts") ?: JSONArray()
                for (i in 0 until parts.length()) merged.put(parts.get(i))
                last.put("parts", merged)
            } else {
                contents.put(JSONObject().put("role", role).put("parts", parts))
            }
        }

        for (m in messages) {
            val role = m.optString("role")
            when (role) {
                "system" -> {
                    val text = Jsons.contentText(m.opt("content"))
                    if (text.isNotBlank()) {
                        if (system.isNotEmpty()) system.append("\n\n")
                        system.append(text)
                    }
                }
                "tool" -> {
                    val parts = JSONArray()
                    val name = toolNames[m.optString("tool_call_id")].orEmpty()
                    val response = JSONObject().put("content", Jsons.contentText(m.opt("content")))
                    parts.put(
                        JSONObject().put(
                            "functionResponse",
                            JSONObject().put("name", name.ifBlank { "tool" }).put("response", response),
                        ),
                    )
                    append("user", parts)
                }
                else -> {
                    val parts = JSONArray()
                    val blocks = m.opt("content")
                    if (blocks is JSONArray) {
                        for (i in 0 until blocks.length()) {
                            val b = blocks.optJSONObject(i) ?: continue
                            when (b.optString("type")) {
                                "text" -> parts.put(JSONObject().put("text", b.optString("text")))
                                "image_url" -> imagePart(b.optJSONObject("image_url")?.optString("url").orEmpty())?.let { parts.put(it) }
                            }
                        }
                    } else {
                        val text = Jsons.contentText(blocks)
                        if (text.isNotBlank()) parts.put(JSONObject().put("text", text))
                    }
                    if (role == "assistant") {
                        m.optJSONArray("tool_calls")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val tc = arr.optJSONObject(i) ?: continue
                                val fn = tc.optJSONObject("function") ?: continue
                                val name = fn.optString("name")
                                val id = tc.optString("id").ifEmpty { "call_" + i }
                                toolNames[id] = name
                                parts.put(
                                    JSONObject().put(
                                        "functionCall",
                                        JSONObject().put("name", name)
                                            .put("args", Jsons.args(fn.optString("arguments")) ?: JSONObject()),
                                    ),
                                )
                            }
                        }
                    }
                    append(if (role == "assistant") "model" else "user", parts)
                }
            }
        }

        val body = JSONObject().put("contents", contents)
        if (system.isNotEmpty()) {
            body.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system.toString()))),
            )
        }
        if (tools.length() > 0) {
            val decls = JSONArray()
            for (i in 0 until tools.length()) {
                val t = tools.optJSONObject(i) ?: continue
                val fn = t.optJSONObject("function") ?: continue
                decls.put(
                    JSONObject().put("name", fn.optString("name"))
                        .put("description", fn.optString("description"))
                        .put("parameters", Jsons.geminiSchema(fn.optJSONObject("parameters"))),
                )
            }
            body.put("tools", JSONArray().put(JSONObject().put("functionDeclarations", decls)))
        }
        val gen = JSONObject()
        temperature?.let { gen.put("temperature", it) }
        topP?.let { gen.put("topP", it) }
        maxOutputTokens?.let { gen.put("maxOutputTokens", it) }
        // thinkingConfig only exists on Gemini 2.5+; sending it to an older model is a 400, so it is gated
        // on the model name rather than sent hopefully.
        if (model.contains("2.5") || model.contains("3.")) {
            gen.put(
                "thinkingConfig",
                JSONObject().put("thinkingBudget", if (thinking) thinkingBudget(reasoningEffort) else 0),
            )
        }
        if (gen.length() > 0) body.put("generationConfig", gen)
        return body
    }

    private fun thinkingBudget(effort: String): Int = when (effort.trim().lowercase()) {
        "low", "minimal" -> 1024
        "medium" -> 4096
        else -> 8192
    }

    private fun imagePart(url: String): JSONObject? {
        if (url.isBlank()) return null
        val data = url.substringAfter("base64,", "")
        if (data.isEmpty()) return null
        val mime = url.substringAfter("data:", "").substringBefore(";").ifBlank { "image/png" }
        return JSONObject().put(
            "inlineData",
            JSONObject().put("mimeType", mime).put("data", data),
        )
    }

    fun parse(root: JSONObject): WireReply {
        val cand = root.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw IllegalStateException("Gemini reply has no candidates: " + root.toString().take(300))
        val parts = cand.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        val text = StringBuilder()
        val reasoning = StringBuilder()
        val calls = mutableListOf<WireToolCall>()
        val canonicalParts = JSONArray()
        for (i in 0 until parts.length()) {
            val p = parts.optJSONObject(i) ?: continue
            if (p.has("text")) {
                if (p.optBoolean("thought")) reasoning.append(p.optString("text"))
                else text.append(p.optString("text"))
                continue
            }
            val fc = p.optJSONObject("functionCall") ?: continue
            val name = fc.optString("name")
            val args = fc.optJSONObject("args") ?: JSONObject()
            val id = "gemini_call_" + calls.size
            calls += WireToolCall(id, name, args.toString())
            canonicalParts.put(
                JSONObject().put("id", id).put("type", "function")
                    .put("function", JSONObject().put("name", name).put("arguments", args.toString())),
            )
        }
        val content = text.toString()
        val assistant = JSONObject().put("role", "assistant")
            .put("content", if (content.isEmpty()) JSONObject.NULL else content)
        if (canonicalParts.length() > 0) assistant.put("tool_calls", canonicalParts)
        val usage = root.optJSONObject("usageMetadata")
        return WireReply(
            content = content,
            reasoning = reasoning.toString(),
            toolCalls = calls,
            assistantMessage = assistant,
            promptTokens = usage?.optInt("promptTokenCount") ?: 0,
            completionTokens = usage?.optInt("candidatesTokenCount") ?: 0,
            cachedTokens = usage?.optInt("cachedContentTokenCount") ?: 0,
        )
    }
}

/** Anthropic's /v1/messages wire. */
object AnthropicWire {

    const val API_VERSION = "2023-06-01"

    fun endpoint(baseUrl: String): String {
        val b = baseUrl.trim().trimEnd('/')
        if (b.endsWith("/messages")) return b
        if (b.endsWith("/v1")) return b + "/messages"
        return b + "/v1/messages"
    }

    fun headers(apiKey: String, extra: Map<String, String>): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["Content-Type"] = "application/json; charset=utf-8"
        h["anthropic-version"] = API_VERSION
        if (apiKey.isNotBlank()) h["x-api-key"] = apiKey.trim()
        for ((k, v) in extra) if (k.isNotBlank() && v.isNotBlank()) h[k.trim()] = v.trim()
        return h
    }

    fun body(
        model: String,
        messages: List<JSONObject>,
        tools: JSONArray,
        thinking: Boolean,
        reasoningEffort: String,
        temperature: Double?,
        topP: Double?,
        maxOutputTokens: Int?,
    ): JSONObject {
        val system = StringBuilder()
        val msgs = JSONArray()

        fun append(role: String, blocks: JSONArray) {
            if (blocks.length() == 0) return
            val last = if (msgs.length() > 0) msgs.optJSONObject(msgs.length() - 1) else null
            if (last != null && last.optString("role") == role) {
                val merged = last.optJSONArray("content") ?: JSONArray()
                for (i in 0 until blocks.length()) merged.put(blocks.get(i))
                last.put("content", merged)
            } else {
                msgs.put(JSONObject().put("role", role).put("content", blocks))
            }
        }

        for (m in messages) {
            val role = m.optString("role")
            when (role) {
                "system" -> {
                    val text = Jsons.contentText(m.opt("content"))
                    if (text.isNotBlank()) {
                        if (system.isNotEmpty()) system.append("\n\n")
                        system.append(text)
                    }
                }
                "tool" -> append(
                    "user",
                    JSONArray().put(
                        JSONObject().put("type", "tool_result")
                            .put("tool_use_id", m.optString("tool_call_id"))
                            .put("content", Jsons.contentText(m.opt("content"))),
                    ),
                )
                else -> {
                    val blocks = JSONArray()
                    val raw = m.opt("content")
                    if (raw is JSONArray) {
                        for (i in 0 until raw.length()) {
                            val b = raw.optJSONObject(i) ?: continue
                            when (b.optString("type")) {
                                "text" -> blocks.put(JSONObject().put("type", "text").put("text", b.optString("text")))
                                "image_url" -> imageBlock(b.optJSONObject("image_url")?.optString("url").orEmpty())?.let { blocks.put(it) }
                            }
                        }
                    } else {
                        val text = Jsons.contentText(raw)
                        if (text.isNotBlank()) blocks.put(JSONObject().put("type", "text").put("text", text))
                    }
                    if (role == "assistant") {
                        m.optJSONArray("tool_calls")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                val tc = arr.optJSONObject(i) ?: continue
                                val fn = tc.optJSONObject("function") ?: continue
                                blocks.put(
                                    JSONObject().put("type", "tool_use")
                                        .put("id", tc.optString("id").ifEmpty { "call_" + i })
                                        .put("name", fn.optString("name"))
                                        .put("input", Jsons.args(fn.optString("arguments")) ?: JSONObject()),
                                )
                            }
                        }
                    }
                    append(if (role == "assistant") "assistant" else "user", blocks)
                }
            }
        }

        // The API requires at least one user message; a turn that only carries a system prompt is still a
        // request the user made, and guessing wrong here is a 400 from the provider.
        if (msgs.length() == 0 || msgs.optJSONObject(0)?.optString("role") == "assistant") {
            msgs.put(
                0,
                JSONObject().put("role", "user").put(
                    "content",
                    JSONArray().put(JSONObject().put("type", "text").put("text", ".")),
                ),
            )
        }

        val useThinking = thinking && tools.length() == 0
        val budget = thinkingBudget(reasoningEffort)
        val maxTokens = maxOutputTokens ?: if (useThinking) maxOf(8192, budget + 2048) else 8192
        val body = JSONObject().put("model", model).put("max_tokens", maxTokens).put("messages", msgs)
        if (system.isNotEmpty()) body.put("system", system.toString())
        if (tools.length() > 0) {
            val arr = JSONArray()
            for (i in 0 until tools.length()) {
                val t = tools.optJSONObject(i) ?: continue
                val fn = t.optJSONObject("function") ?: continue
                arr.put(
                    JSONObject().put("name", fn.optString("name"))
                        .put("description", fn.optString("description"))
                        .put("input_schema", fn.optJSONObject("parameters") ?: JSONObject().put("type", "object")),
                )
            }
            body.put("tools", arr)
        }
        if (useThinking) {
            body.put("thinking", JSONObject().put("type", "enabled").put("budget_tokens", budget))
            // Extended thinking is incompatible with a temperature other than 1; the API rejects the request
            // rather than ignoring it.
            body.put("temperature", 1.0)
        } else {
            temperature?.let { body.put("temperature", it) }
            topP?.let { body.put("top_p", it) }
        }
        return body
    }

    private fun thinkingBudget(effort: String): Int = when (effort.trim().lowercase()) {
        "low", "minimal" -> 1024
        "medium" -> 4096
        else -> 8192
    }

    private fun imageBlock(url: String): JSONObject? {
        val data = url.substringAfter("base64,", "")
        if (data.isEmpty()) return null
        val mime = url.substringAfter("data:", "").substringBefore(";").ifBlank { "image/png" }
        return JSONObject().put("type", "image").put(
            "source",
            JSONObject().put("type", "base64").put("media_type", mime).put("data", data),
        )
    }

    fun parse(root: JSONObject): WireReply {
        val blocks = root.optJSONArray("content")
            ?: throw IllegalStateException("Claude reply has no content: " + root.toString().take(300))
        val text = StringBuilder()
        val reasoning = StringBuilder()
        val calls = mutableListOf<WireToolCall>()
        val canonical = JSONArray()
        for (i in 0 until blocks.length()) {
            val b = blocks.optJSONObject(i) ?: continue
            when (b.optString("type")) {
                "text" -> text.append(b.optString("text"))
                "thinking" -> reasoning.append(b.optString("thinking"))
                "tool_use" -> {
                    val id = b.optString("id").ifEmpty { "call_" + i }
                    val name = b.optString("name")
                    val input = b.optJSONObject("input") ?: JSONObject()
                    calls += WireToolCall(id, name, input.toString())
                    canonical.put(
                        JSONObject().put("id", id).put("type", "function")
                            .put("function", JSONObject().put("name", name).put("arguments", input.toString())),
                    )
                }
            }
        }
        val content = text.toString()
        val assistant = JSONObject().put("role", "assistant")
            .put("content", if (content.isEmpty()) JSONObject.NULL else content)
        if (canonical.length() > 0) assistant.put("tool_calls", canonical)
        val usage = root.optJSONObject("usage")
        return WireReply(
            content = content,
            reasoning = reasoning.toString(),
            toolCalls = calls,
            assistantMessage = assistant,
            promptTokens = usage?.optInt("input_tokens") ?: 0,
            completionTokens = usage?.optInt("output_tokens") ?: 0,
            cachedTokens = usage?.optInt("cache_read_input_tokens") ?: 0,
        )
    }
}

/** Provider-independent classification of a failed call, used to decide retry and fallback. */
enum class LlmErrorKind { AUTH, QUOTA, RATE_LIMIT, NETWORK, SERVER, REQUEST, UNKNOWN }

object LlmErrors {
    fun classify(code: Int): LlmErrorKind = when (code) {
        401, 403 -> LlmErrorKind.AUTH
        402 -> LlmErrorKind.QUOTA
        429 -> LlmErrorKind.RATE_LIMIT
        in 500..599 -> LlmErrorKind.SERVER
        in 400..499 -> LlmErrorKind.REQUEST
        else -> LlmErrorKind.UNKNOWN
    }

    /** Worth asking the same provider again: a rate limit or a server hiccup is often gone in a second. */
    fun retryable(kind: LlmErrorKind): Boolean = when (kind) {
        LlmErrorKind.RATE_LIMIT, LlmErrorKind.NETWORK, LlmErrorKind.SERVER -> true
        else -> false
    }

    /**
     * Worth asking a *different* provider: auth, quota and rate limit are per-account facts a fallback can
     * route around; a malformed request (400) would fail everywhere and is returned as-is instead of
     * spending the user's fallback quota on it.
     */
    fun fallbackEligible(kind: LlmErrorKind): Boolean = kind != LlmErrorKind.REQUEST
}
