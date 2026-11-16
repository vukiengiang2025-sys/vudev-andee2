package net.kuafuai.andee.config

import android.content.Context
import net.kuafuai.andee.brain.LlmHttpException
import net.kuafuai.andee.brain.provider.AiProvider
import net.kuafuai.andee.brain.provider.AiProviders
import net.kuafuai.andee.brain.provider.ModelRole
import net.kuafuai.andee.brain.provider.ProviderEndpoint
import net.kuafuai.andee.security.SecretBox
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * The AI Provider Center's storage: the list of configured providers, which one answers, which one
 * stands in when it fails, and which model covers each task.
 *
 * ### Why this is not in [VoiceConfig]
 *
 * VoiceConfig's save path drops blank values and coerces every row to a String — a rule that suits a
 * settings form and does not suit a list of records with optional numbers and secrets. Keeping the
 * records in their own file also means a provider list survives a "reset settings to defaults" that
 * should not cost the user every key they typed.
 *
 * ### Keys never sit in plain text
 *
 * Every key is sealed with [SecretBox] (AES/GCM, key in the Android Keystore) before it is written.
 * [keyOf] decrypts on demand and returns "" when the blob belongs to another install, which the UI
 * reports as "type the key again" rather than as an empty provider. The legacy plain-text llm_api_key is
 * migrated into a sealed record and then deleted on first run — see [seedIfNeeded].
 *
 * ### The active provider feeds BrainConfig
 *
 * [VoiceConfig.brainConfig] asks this store first and falls back to the legacy fields only when the store
 * has never been seeded. That is what makes the provider switch a settings change rather than a code path
 * the brain has to know about.
 */
object ProviderStore {

    private const val PREFS = "andee_ai"
    private const val KEY_STATE = "state"
    private const val VOICE_PREFS = "voice_prefs"

    /** One configured provider: a provider type, where it lives, which model, and the user's dials. */
    data class Entry(
        val id: String,
        val providerId: String,
        val label: String,
        val baseUrl: String,
        val model: String,
        val keySealed: String = "",
        val enabled: Boolean = true,
        val thinking: Boolean = false,
        val reasoningEffort: String = VoiceConfig.DEFAULT_REASONING_EFFORT,
        val temperature: Double? = null,
        val topP: Double? = null,
        val maxOutputTokens: Int? = null,
        val timeoutSeconds: Long = 180,
        val maxRetries: Int = 1,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        val provider: AiProvider get() = AiProviders.providerFor(providerId, baseUrl)
        val hasKey: Boolean get() = keySealed.isNotBlank()
        val resolvedModel: String get() = model.trim().ifEmpty { provider.defaultModel }
        val resolvedBaseUrl: String get() = baseUrl.trim().ifEmpty { provider.defaultBaseUrl }
    }

    data class TestResult(val ok: Boolean, val detail: String, val latencyMs: Long)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun state(context: Context): JSONObject =
        runCatching { JSONObject(prefs(context).getString(KEY_STATE, "") ?: "") }.getOrDefault(JSONObject())

    private fun write(context: Context, root: JSONObject) {
        prefs(context).edit().putString(KEY_STATE, root.toString()).apply()
    }

    // ---- seeding -------------------------------------------------------------------------------------

    /**
     * Turn the legacy single-provider settings into the first record, once, and delete the plain-text key.
     *
     * Runs on every read and returns immediately after the first write. A device that never typed a key
     * still gets one entry (DeepSeek with no key) so the Provider Center has something to show and the
     * brain keeps failing with "no key" exactly as before.
     */
    @Synchronized
    fun seedIfNeeded(context: Context) {
        val root = state(context)
        if (root.optJSONArray("providers")?.length() ?: 0 > 0) return
        val p = context.getSharedPreferences(VOICE_PREFS, Context.MODE_PRIVATE)
        val legacyKey = p.getString("llm_api_key", "").orEmpty().trim()
        val legacyBase = p.getString("llm_base_url", "").orEmpty().trim()
            .ifEmpty { VoiceConfig.llmBaseUrlDefault(context) }
        val legacyModel = p.getString("llm_model", "").orEmpty().trim()
            .ifEmpty { VoiceConfig.llmModelDefault(context) }
        val provider = AiProviders.match(legacyBase)
        val entry = Entry(
            id = newId(),
            providerId = provider.id,
            label = provider.label,
            baseUrl = if (legacyBase == provider.defaultBaseUrl) "" else legacyBase,
            model = if (legacyModel == provider.defaultModel) "" else legacyModel,
            keySealed = SecretBox.seal(legacyKey).orEmpty(),
            thinking = p.getString("llm_thinking", "").orEmpty().trim().lowercase() != "disabled",
            reasoningEffort = p.getString("llm_reasoning_effort", "").orEmpty().trim()
                .ifEmpty { VoiceConfig.DEFAULT_REASONING_EFFORT },
        )
        root.put("active", entry.id)
        root.put("providers", JSONArray().put(entryJson(entry)))
        write(context, root)
        // The key now lives encrypted in the provider record. Leaving a copy in plain text is the exact
        // thing this store exists to prevent.
        if (legacyKey.isNotEmpty()) p.edit().remove("llm_api_key").apply()
    }

    private fun newId(): String = "p_" + System.currentTimeMillis().toString(36) + "_" +
        UUID.randomUUID().toString().take(6)

    // ---- reading -------------------------------------------------------------------------------------

    fun entries(context: Context): List<Entry> {
        seedIfNeeded(context)
        val arr = state(context).optJSONArray("providers") ?: return emptyList()
        val out = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += entryFrom(o)
        }
        return out
    }

    fun entry(context: Context, id: String?): Entry? =
        entries(context).firstOrNull { it.id == id }

    fun activeId(context: Context): String {
        seedIfNeeded(context)
        return state(context).optString("active").orEmpty()
    }

    fun active(context: Context): Entry? {
        val e = entry(context, activeId(context))
        return if (e != null && e.enabled) e else null
    }

    fun fallbackId(context: Context): String = state(context).optString("fallback").orEmpty()

    fun fallback(context: Context): Entry? {
        val e = entry(context, fallbackId(context))
        return if (e != null && e.enabled && e.id != activeId(context)) e else null
    }

    /** The decrypted key for a record, or "" when this install cannot read it. Never log the result. */
    fun keyOf(entry: Entry): String = SecretBox.open(entry.keySealed).orEmpty()

    fun endpointOf(context: Context, entry: Entry, modelOverride: String? = null): ProviderEndpoint =
        ProviderEndpoint(
            providerId = entry.providerId,
            baseUrl = entry.resolvedBaseUrl,
            model = modelOverride?.trim()?.ifEmpty { null } ?: entry.model,
            apiKey = keyOf(entry),
            thinking = entry.thinking,
            reasoningEffort = entry.reasoningEffort,
            temperature = entry.temperature,
            topP = entry.topP,
            maxOutputTokens = entry.maxOutputTokens,
            timeoutSeconds = entry.timeoutSeconds,
            maxRetries = entry.maxRetries,
        )

    /**
     * The endpoint the conversation should use, or null when the store has nothing usable (which only
     * happens if the user deleted every record; the legacy fields then answer, as they did before).
     */
    fun activeEndpoint(context: Context): ProviderEndpoint? =
        active(context)?.let { endpointOf(context, it) }

    /** The fallback endpoint, or null when none is configured. */
    fun fallbackEndpoint(context: Context): ProviderEndpoint? =
        fallback(context)?.let { endpointOf(context, it) }

    fun roleEntryId(context: Context, role: ModelRole): String {
        val roles = state(context).optJSONObject("roles") ?: return ""
        return roles.optJSONObject(role.id)?.optString("entry").orEmpty()
    }

    fun roleModelOverride(context: Context, role: ModelRole): String {
        val roles = state(context).optJSONObject("roles") ?: return ""
        return roles.optJSONObject(role.id)?.optString("model").orEmpty()
    }

    /**
     * The endpoint for one task. MAIN is the active provider; FALLBACK is the stand-in; every other role
     * is that role's record, or the active provider when the user never chose one for it.
     */
    fun endpointForRole(context: Context, role: ModelRole): ProviderEndpoint? {
        if (role == ModelRole.MAIN) return activeEndpoint(context)
        if (role == ModelRole.FALLBACK) return fallbackEndpoint(context)
        val id = roleEntryId(context, role)
        val e = entry(context, id) ?: active(context) ?: return null
        val model = roleModelOverride(context, role).ifEmpty { null }
        return endpointOf(context, e, model)
    }

    // ---- writing -------------------------------------------------------------------------------------

    /**
     * Create or update a record. [plainKey] null keeps the stored key (the UI leaves the field empty when
     * it is not being changed); a non-blank value replaces it, and the empty string clears it.
     */
    @Synchronized
    fun save(context: Context, entry: Entry, plainKey: String? = null): Entry {
        seedIfNeeded(context)
        val root = state(context)
        val arr = root.optJSONArray("providers") ?: JSONArray()
        val saved = if (plainKey == null) entry.copy(keySealed = entry.keySealed)
        else entry.copy(keySealed = SecretBox.seal(plainKey).orEmpty())
        var replaced = false
        for (i in 0 until arr.length()) {
            if (arr.optJSONObject(i)?.optString("id") == saved.id) {
                arr.put(i, entryJson(saved)); replaced = true; break
            }
        }
        if (!replaced) arr.put(entryJson(saved))
        root.put("providers", arr)
        if (root.optString("active").isBlank()) root.put("active", saved.id)
        write(context, root)
        return entry(context, saved.id) ?: saved
    }

    fun setActive(context: Context, id: String) {
        val root = state(context)
        root.put("active", id)
        if (root.optString("fallback") == id) root.remove("fallback")
        write(context, root)
    }

    fun setFallback(context: Context, id: String) {
        val root = state(context)
        if (id.isBlank() || id == root.optString("active")) root.remove("fallback") else root.put("fallback", id)
        write(context, root)
    }

    fun setEnabled(context: Context, id: String, on: Boolean) {
        val e = entry(context, id) ?: return
        save(context, e.copy(enabled = on), null)
    }

    fun setRole(context: Context, role: ModelRole, entryId: String, model: String = "") {
        val root = state(context)
        val roles = root.optJSONObject("roles") ?: JSONObject()
        if (entryId.isBlank()) roles.remove(role.id)
        else roles.put(role.id, JSONObject().put("entry", entryId).put("model", model.trim()))
        root.put("roles", roles)
        write(context, root)
    }

    /** Delete a record. Roles and the fallback pointing at it are cleared with it; the active becomes the first left. */
    @Synchronized
    fun remove(context: Context, id: String) {
        val root = state(context)
        val arr = root.optJSONArray("providers") ?: return
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") != id) kept.put(o)
        }
        root.put("providers", kept)
        if (root.optString("active") == id) {
            val first = kept.optJSONObject(0)?.optString("id").orEmpty()
            if (first.isEmpty()) root.remove("active") else root.put("active", first)
        }
        if (root.optString("fallback") == id) root.remove("fallback")
        root.optJSONObject("roles")?.let { roles ->
            for (k in roles.keys().asSequence().toList()) {
                if (roles.optJSONObject(k)?.optString("entry") == id) roles.remove(k)
            }
        }
        write(context, root)
    }

    /**
     * Ask the provider to answer. Blocking; call from a worker thread. A reply of any content counts as
     * success: the point is that the URL, the key and the model are accepted together.
     */
    fun test(entry: Entry, plainKey: String): TestResult {
        val started = System.currentTimeMillis()
        return try {
            val ep = ProviderEndpoint(
                providerId = entry.providerId,
                baseUrl = entry.resolvedBaseUrl,
                model = entry.model,
                apiKey = plainKey,
                thinking = false,
                reasoningEffort = "low",
                maxOutputTokens = 16,
                timeoutSeconds = 40,
                maxRetries = 0,
            )
            val reply = ep.client(timeoutSeconds = 40).complete(
                listOf(JSONObject().put("role", "user").put("content", "ping")),
                JSONArray(),
                noThinking = true,
            )
            val words = reply.content.trim().take(80)
            TestResult(
                ok = true,
                detail = words.ifEmpty { "kết nối OK" },
                latencyMs = System.currentTimeMillis() - started,
            )
        } catch (e: LlmHttpException) {
            val why = e.detail.ifEmpty { e.body.take(200) }
            TestResult(false, httpHint(e.code) + " — " + why, System.currentTimeMillis() - started)
        } catch (e: net.kuafuai.andee.brain.LlmMovedException) {
            TestResult(false, "Địa chỉ trả về trang khác hoặc chuyển hướng (HTTP " + e.code + "). Kiểm tra Base URL.", System.currentTimeMillis() - started)
        } catch (e: Exception) {
            TestResult(false, (e.message ?: e.javaClass.simpleName).take(240), System.currentTimeMillis() - started)
        }
    }

    /** A sentence a person can act on, for the HTTP codes that mean something specific. */
    fun httpHint(code: Int): String = when (code) {
        401, 403 -> "API key bị từ chối (HTTP " + code + ")"
        402 -> "Tài khoản hết số dư (HTTP 402)"
        404 -> "Không tìm thấy model hoặc địa chỉ (HTTP 404)"
        429 -> "Bị giới hạn tần suất, thử lại sau (HTTP 429)"
        in 500..599 -> "Nhà cung cấp đang lỗi (HTTP " + code + ")"
        else -> "Lỗi HTTP " + code
    }

    // ---- JSON ----------------------------------------------------------------------------------------

    private fun entryJson(e: Entry): JSONObject {
        val o = JSONObject()
            .put("id", e.id)
            .put("provider", e.providerId)
            .put("label", e.label)
            .put("base_url", e.baseUrl)
            .put("model", e.model)
            .put("key", e.keySealed)
            .put("enabled", e.enabled)
            .put("thinking", e.thinking)
            .put("effort", e.reasoningEffort)
            .put("timeout", e.timeoutSeconds)
            .put("retries", e.maxRetries)
            .put("created_at", e.createdAt)
        e.temperature?.let { o.put("temperature", it) }
        e.topP?.let { o.put("top_p", it) }
        e.maxOutputTokens?.let { o.put("max_tokens", it) }
        return o
    }

    private fun entryFrom(o: JSONObject): Entry = Entry(
        id = o.optString("id"),
        providerId = o.optString("provider"),
        label = o.optString("label"),
        baseUrl = o.optString("base_url"),
        model = o.optString("model"),
        keySealed = o.optString("key"),
        enabled = o.optBoolean("enabled", true),
        thinking = o.optBoolean("thinking", false),
        reasoningEffort = o.optString("effort", VoiceConfig.DEFAULT_REASONING_EFFORT),
        temperature = if (o.has("temperature")) o.optDouble("temperature") else null,
        topP = if (o.has("top_p")) o.optDouble("top_p") else null,
        maxOutputTokens = if (o.has("max_tokens")) o.optInt("max_tokens") else null,
        timeoutSeconds = o.optLong("timeout", 180),
        maxRetries = o.optInt("retries", 1),
        createdAt = o.optLong("created_at", 0L),
    )
}
