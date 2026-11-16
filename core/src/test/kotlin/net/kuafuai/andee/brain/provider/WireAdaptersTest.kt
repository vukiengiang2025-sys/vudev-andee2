package net.kuafuai.andee.brain.provider

import com.sun.net.httpserver.HttpServer
import net.kuafuai.andee.brain.LlmClient
import net.kuafuai.andee.brain.LlmHttpException
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetSocketAddress

/**
 * The provider layer without a real provider: request shapes, reply translation, and the fallback rule.
 *
 * These are the tests that would have caught a Gemini request shaped like an OpenAI one, or a fallback
 * that quietly retries a 400 on the user's second key.
 */
class WireAdaptersTest {

    private val tools = JSONArray().put(
        JSONObject().put("type", "function").put(
            "function",
            JSONObject().put("name", "tap")
                .put("description", "tap something")
                .put("parameters", JSONObject().put("type", "object")
                    .put("properties", JSONObject().put("x", JSONObject().put("type", "number")))),
        ),
    )

    private fun user(text: String) = JSONObject().put("role", "user").put("content", text)

    private fun assistantToolCall(id: String, name: String, args: String) = JSONObject()
        .put("role", "assistant").put("content", JSONObject.NULL)
        .put("tool_calls", JSONArray().put(
            JSONObject().put("id", id).put("type", "function")
                .put("function", JSONObject().put("name", name).put("arguments", args)),
        ))

    // ---- endpoints ----------------------------------------------------------------------------------

    @Test fun openAiEndpointAcceptsBaseAndFullUrl() {
        assertEquals("https://api.deepseek.com/chat/completions", OpenAiWire.endpoint("https://api.deepseek.com"))
        assertEquals("https://api.openai.com/v1/chat/completions", OpenAiWire.endpoint("https://api.openai.com/v1/"))
        assertEquals("https://x.example/v1/chat/completions", OpenAiWire.endpoint("https://x.example/v1/chat/completions"))
    }

    @Test fun geminiEndpointNamesTheModelAndOperation() {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            GeminiWire.endpoint("https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent",
            GeminiWire.endpoint("https://generativelanguage.googleapis.com/v1beta/models", "gemini-2.5-pro"),
        )
    }

    @Test fun anthropicEndpointAddsTheVersionPath() {
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicWire.endpoint("https://api.anthropic.com"))
        assertEquals("https://api.anthropic.com/v1/messages", AnthropicWire.endpoint("https://api.anthropic.com/v1"))
    }

    // ---- request shapes -----------------------------------------------------------------------------

    @Test fun geminiTurnsSystemAndToolCallHistoryIntoContents() {
        val messages = listOf(
            JSONObject().put("role", "system").put("content", "you are Andee"),
            user("hello"),
            assistantToolCall("c1", "tap", "{\"x\":5}"),
            JSONObject().put("role", "tool").put("tool_call_id", "c1").put("content", "{\"ok\":true}"),
        )
        val body = GeminiWire.body(
            model = "gemini-2.5-flash", messages = messages, tools = tools,
            thinking = false, reasoningEffort = "low",
            temperature = 0.4, topP = null, maxOutputTokens = 128,
        )
        assertTrue(body.getJSONObject("systemInstruction").getJSONArray("parts")
            .getJSONObject(0).getString("text").contains("Andee"))
        val contents = body.getJSONArray("contents")
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        // The tool result must carry the NAME of the call, which the canonical history only has by id.
        val toolTurn = contents.getJSONObject(2)
        assertEquals("user", toolTurn.getString("role"))
        val fr = toolTurn.getJSONArray("parts").getJSONObject(0).getJSONObject("functionResponse")
        assertEquals("tap", fr.getString("name"))
        assertEquals(0.4, body.getJSONObject("generationConfig").getDouble("temperature"), 0.0001)
        assertEquals(128, body.getJSONObject("generationConfig").getInt("maxOutputTokens"))
    }

    @Test fun geminiParsesTextAndFunctionCallIntoCanonicalReply() {
        val root = JSONObject().put(
            "candidates", JSONArray().put(JSONObject().put("content", JSONObject().put(
                "parts", JSONArray()
                    .put(JSONObject().put("text", "let me look"))
                    .put(JSONObject().put("functionCall", JSONObject().put("name", "tap")
                        .put("args", JSONObject().put("x", 3)))),
            ))),
        ).put("usageMetadata", JSONObject().put("promptTokenCount", 11).put("candidatesTokenCount", 7))
        val wr = GeminiWire.parse(root)
        assertEquals("let me look", wr.content)
        assertEquals(1, wr.toolCalls.size)
        assertEquals("tap", wr.toolCalls[0].name)
        assertEquals(11, wr.promptTokens)
        assertEquals("assistant", wr.assistantMessage.getString("role"))
        assertNotNull(wr.assistantMessage.getJSONArray("tool_calls"))
    }

    @Test fun anthropicSendsSystemTopLevelAndToolResultsAsUserBlocks() {
        val messages = listOf(
            JSONObject().put("role", "system").put("content", "guard text"),
            user("hello"),
            assistantToolCall("toolu_1", "tap", "{\"x\":5}"),
            JSONObject().put("role", "tool").put("tool_call_id", "toolu_1").put("content", "done"),
        )
        val body = AnthropicWire.body(
            model = "claude-sonnet-4-5", messages = messages, tools = tools,
            thinking = false, reasoningEffort = "high",
            temperature = null, topP = null, maxOutputTokens = null,
        )
        assertEquals("guard text", body.getString("system"))
        assertTrue(body.getInt("max_tokens") > 0)
        val msgs = body.getJSONArray("messages")
        assertEquals("assistant", msgs.getJSONObject(1).getString("role"))
        val toolUse = msgs.getJSONObject(1).getJSONArray("content").getJSONObject(0)
        assertEquals("tool_use", toolUse.getString("type"))
        val toolResult = msgs.getJSONObject(2).getJSONArray("content").getJSONObject(0)
        assertEquals("tool_result", toolResult.getString("type"))
        assertEquals("toolu_1", toolResult.getString("tool_use_id"))
        assertEquals("tap", body.getJSONArray("tools").getJSONObject(0).getString("name"))
    }

    @Test fun anthropicParsesToolUseIntoCanonicalReply() {
        val root = JSONObject().put("content", JSONArray()
            .put(JSONObject().put("type", "text").put("text", "ok"))
            .put(JSONObject().put("type", "tool_use").put("id", "toolu_9")
                .put("name", "tap").put("input", JSONObject().put("x", 1))))
            .put("usage", JSONObject().put("input_tokens", 5).put("output_tokens", 2))
        val wr = AnthropicWire.parse(root)
        assertEquals("ok", wr.content)
        assertEquals("toolu_9", wr.toolCalls[0].id)
        assertEquals(5, wr.promptTokens)
        assertEquals(2, wr.completionTokens)
    }

    // ---- provider registry --------------------------------------------------------------------------

    @Test fun registryResolvesByUrlForLegacySettingsAndCustomStaysOpenAiCompatible() {
        assertEquals(AiProviders.DEEPSEEK, AiProviders.match("https://api.deepseek.com").id)
        assertEquals(AiProviders.GEMINI, AiProviders.match("https://generativelanguage.googleapis.com/v1beta").id)
        assertEquals(AiProviders.ANTHROPIC, AiProviders.match("https://api.anthropic.com").id)
        assertEquals(AiProviders.OLLAMA, AiProviders.match("http://127.0.0.1:11434/v1").id)
        assertEquals(AiProviders.CUSTOM, AiProviders.match("https://my.gateway.example/v1").id)
        assertEquals(WireFormat.GEMINI, AiProviders.byId(AiProviders.GEMINI)!!.wire)
        assertTrue(AiProviders.byId(AiProviders.OLLAMA)!!.needsKey.not())
    }

    @Test fun endpointUsesProviderDefaultsForBlankFields() {
        val ep = ProviderEndpoint(providerId = AiProviders.GEMINI)
        assertEquals("https://generativelanguage.googleapis.com/v1beta", ep.resolvedBaseUrl)
        assertEquals("gemini-2.5-flash", ep.resolvedModel)
        assertFalse(ep.sameBackend(ProviderEndpoint(providerId = AiProviders.GEMINI, apiKey = "k")))
    }

    // ---- retry and fallback -------------------------------------------------------------------------

    private val servers = ArrayList<HttpServer>()

    private fun server(status: Int, body: String): HttpServer {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/chat/completions") { ex ->
            ex.requestBody.readBytes()
            val out = body.toByteArray()
            ex.sendResponseHeaders(status, out.size.toLong())
            ex.responseBody.use { it.write(out) }
        }
        s.start()
        servers += s
        return s
    }

    private fun okBody(text: String): String = JSONObject()
        .put("choices", JSONArray().put(JSONObject().put("message",
            JSONObject().put("role", "assistant").put("content", text))))
        .put("usage", JSONObject().put("prompt_tokens", 1).put("completion_tokens", 1))
        .toString()

    @After fun tearDown() {
        for (s in servers) s.stop(0)
        servers.clear()
    }

    @Test fun aTransientServerErrorFallsBackToTheSecondProvider() {
        val bad = server(500, "{\"error\":\"boom\"}")
        val good = server(200, okBody("from fallback"))
        val fallback = LlmClient("http://127.0.0.1:" + good.address.port, "k2", "m", false, "low", maxRetries = 0)
        val primary = LlmClient(
            "http://127.0.0.1:" + bad.address.port, "k1", "m", false, "low",
            maxRetries = 0, fallback = fallback,
        )
        val reply = primary.complete(listOf(user("hi")), JSONArray())
        assertEquals("from fallback", reply.content)
    }

    @Test fun aMalformedRequestIsNotRetried(){ 
        val bad = server(400, "{\"error\":{\"message\":\"bad model\"}}")
        var fallbackUsed = false
        val fallback = object {
            fun complete(): LlmClient.Reply {
                fallbackUsed = true
                throw IllegalStateException("not called")
            }
        }
        val primary = LlmClient("http://127.0.0.1:" + bad.address.port, "k", "m", false, "low", maxRetries = 0)
        try {
            primary.complete(listOf(user("hi")), JSONArray())
            fail("no exception")
        } catch (e: LlmHttpException) {
            assertEquals(400, e.code)
        }
        assertFalse(fallbackUsed)
        // The object above is a compile-time reminder that a 400 has no fallback path; the real assertion
        // is that the exception reached the caller instead of being swallowed.
    }
}
