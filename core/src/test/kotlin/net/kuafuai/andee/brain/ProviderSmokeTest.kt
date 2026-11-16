package net.kuafuai.andee.brain

import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.net.InetSocketAddress

class ProviderSmokeTest {
    @Test fun localOpenAiShapedServerAnswers() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/chat/completions") { ex ->
                ex.requestBody.readBytes()
                val out = JSONObject()
                    .put("choices", JSONArray().put(JSONObject().put("message",
                        JSONObject().put("role", "assistant").put("content", "xin chào"))))
                    .put("usage", JSONObject().put("prompt_tokens", 1).put("completion_tokens", 1))
                    .toString().toByteArray()
                ex.sendResponseHeaders(200, out.size.toLong())
                ex.responseBody.use { it.write(out) }
            }
            start()
        }
        try {
            val c = LlmClient("http://127.0.0.1:" + server.address.port, "key", "m", false, "low")
            val r = c.complete(listOf(JSONObject().put("role", "user").put("content", "hi")), JSONArray())
            println("SMOKE content=" + r.content + " calls=" + r.toolCalls.size + " provider=" + c.provider.id)
        } catch (e: Throwable) {
            println("SMOKE FAILED: " + e)
            e.printStackTrace()
            throw e
        } finally {
            server.stop(0)
        }
    }
}
