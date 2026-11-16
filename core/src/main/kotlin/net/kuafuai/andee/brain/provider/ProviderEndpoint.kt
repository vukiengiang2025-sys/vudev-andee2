package net.kuafuai.andee.brain.provider

import net.kuafuai.andee.brain.LlmClient

/**
 * The roles a model can be chosen for. The user may point each at a different provider and model, which is
 * what makes a cheap model cover background triage while a careful one answers the user.
 *
 * The ids are persisted, so they are stable; the labels live in the app's resources.
 */
enum class ModelRole(val id: String) {
    MAIN("main"),
    FALLBACK("fallback"),
    CODE("code"),
    CHAT("chat"),
    VISION("vision"),
    LONG("long"),
    CHEAP("cheap"),
    ;

    companion object {
        fun byId(id: String?): ModelRole? = entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }
    }
}

/**
 * One configured endpoint: a provider, a URL, a key and the knobs the user chose. This is the value that
 * travels from settings into the brain; it is deliberately independent of Android so the desktop body and
 * the unit tests can build one.
 *
 * A key is held here in memory only. The app stores it encrypted (see the phone's SecretBox) and hands it
 * over when it builds this value; nothing serializes it as-is.
 */
data class ProviderEndpoint(
    val providerId: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val thinking: Boolean = false,
    val reasoningEffort: String = "high",
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
    val timeoutSeconds: Long = 180,
    val maxRetries: Int = 1,
    val headers: Map<String, String> = emptyMap(),
) {
    val provider: AiProvider get() = AiProviders.providerFor(providerId, baseUrl)

    /** The URL actually used: a blank field means the provider's own default, not "no address". */
    val resolvedBaseUrl: String get() = baseUrl.trim().ifEmpty { provider.defaultBaseUrl }.trimEnd('/')

    /** The model actually used: a blank field means the provider's default model. */
    val resolvedModel: String get() = model.trim().ifEmpty { provider.defaultModel }

    /** A model id the provider offers, or null for free-form providers where any id is accepted. */
    fun knownModel(): ModelInfo? = provider.model(resolvedModel)

    /**
     * Whether two endpoints are the same backend asked differently. The key, the provider and the URL are
     * identity; model/thinking/effort are only how it is asked, and a change to those must not discard the
     * conversation (see LocalBrain.retune).
     */
    fun sameBackend(other: ProviderEndpoint): Boolean =
        provider.id == other.provider.id && resolvedBaseUrl == other.resolvedBaseUrl && apiKey == other.apiKey

    /**
     * A client for this endpoint. [modelOverride] is for the calls that deliberately use a different model
     * than the conversation's (the locator's vision model, for example).
     */
    fun client(
        modelOverride: String? = null,
        timeoutSeconds: Long = this.timeoutSeconds,
        fallback: LlmClient? = null,
    ): LlmClient = LlmClient(
        baseUrl = resolvedBaseUrl,
        apiKey = apiKey,
        model = modelOverride?.trim()?.ifEmpty { null } ?: resolvedModel,
        thinking = thinking,
        reasoningEffort = reasoningEffort,
        readTimeoutSeconds = timeoutSeconds,
        providerId = provider.id,
        temperature = temperature,
        topP = topP,
        maxOutputTokens = maxOutputTokens,
        extraHeaders = headers,
        maxRetries = maxRetries,
        fallback = fallback,
    )

    /** Never let a key reach a log line, a crash report or a toString(). */
    override fun toString(): String =
        "ProviderEndpoint(provider=" + provider.id +
            ", model=" + resolvedModel +
            ", url=" + resolvedBaseUrl +
            ", key=" + (if (apiKey.isBlank()) "none" else "set") + ")"

    companion object {
        /** The endpoint a pre-provider settings file describes: DeepSeek at the old llm_* fields. */
        fun legacy(baseUrl: String, apiKey: String, model: String, thinking: Boolean, reasoningEffort: String) =
            ProviderEndpoint(
                providerId = AiProviders.match(baseUrl).id,
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                thinking = thinking,
                reasoningEffort = reasoningEffort,
            )
    }
}
