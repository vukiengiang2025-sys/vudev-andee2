package net.kuafuai.andee.brain

import net.kuafuai.andee.config.Ages
import net.kuafuai.andee.pet.PetCharacters
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekLlmClientTest {

    private val userMsg = listOf(JSONObject().put("role", "user").put("content", "xin chào"))
    private val emptyTools = JSONArray()

    @Test
    fun deepSeekChatThinkingOmitsReasoningEffort() {
        val client = LlmClient("https://api.deepseek.com", "sk-test", "deepseek-chat", thinking = true, reasoningEffort = "low")
        val body = client.buildRequestBody(userMsg, emptyTools)
        assertEquals("enabled", body.getJSONObject("thinking").getString("type"))
        assertFalse(body.has("reasoning_effort"))
    }

    @Test
    fun deepSeekReasonerNoThinkingOmitsDisabledThinkingBlock() {
        val client = LlmClient("https://api.deepseek.com", "sk-test", "deepseek-reasoner", thinking = false, reasoningEffort = "low")
        val body = client.buildRequestBody(userMsg, emptyTools, noThinking = true)
        assertFalse(body.has("thinking"))
        assertFalse(body.has("reasoning_effort"))
    }

    @Test
    fun deepSeekChatNoThinkingDisablesThinking() {
        val client = LlmClient("https://api.deepseek.com", "sk-test", "deepseek-chat", thinking = false, reasoningEffort = "low")
        val body = client.buildRequestBody(userMsg, emptyTools)
        assertEquals("disabled", body.getJSONObject("thinking").getString("type"))
    }

    @Test
    fun deepSeekSanitizeAssistantPreservesReasoningContent() {
        val client = LlmClient("https://api.deepseek.com/v1", "sk-test", "deepseek-chat", thinking = true, reasoningEffort = "low")
        val msg = JSONObject()
            .put("role", "assistant")
            .put("content", "")
            .put("reasoning_content", "step by step")
            .put("tool_calls", JSONArray().put(JSONObject().put("id", "call_1")))
        val sanitized = client.sanitizeAssistant(msg)
        assertEquals("step by step", sanitized.getString("reasoning_content"))
        assertEquals(JSONObject.NULL, sanitized.get("content"))
        assertEquals(1, sanitized.getJSONArray("tool_calls").length())
    }

    @Test
    fun nonDeepSeekGatewayKeepsReasoningEffortAndStripsReasoningContent() {
        val client = LlmClient("https://ark.cn-beijing.volces.com/api/v3", "k", "m", thinking = true, reasoningEffort = "low")
        val body = client.buildRequestBody(userMsg, emptyTools)
        assertEquals("low", body.getString("reasoning_effort"))

        val msg = JSONObject()
            .put("role", "assistant")
            .put("content", "ok")
            .put("reasoning_content", "hidden")
        val sanitized = client.sanitizeAssistant(msg)
        assertFalse(sanitized.has("reasoning_content"))
    }

    @Test
    fun vietnameseHelpersReturnVietnameseCopy() {
        assertEquals("vừa xong", Ages.ago(5_000L, "vi"))
        assertEquals("5 phút trước", Ages.ago(5 * 60_000L, "vi"))
        assertTrue(PetCharacters.byId("andee")!!.label("vi").contains("Andee"))
        assertTrue(PetCharacters.blurb("cat", "vi").isNotBlank())
    }
}
