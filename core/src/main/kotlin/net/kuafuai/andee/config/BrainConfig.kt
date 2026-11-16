package net.kuafuai.andee.config

/**
 * Which brain answers, and how to reach it if it is the local one.
 *
 * Lives in the shared module because both the phone and the desktop run the
 * same brain and need the same answer to "is this a different backend or the
 * same one asked differently". Android still reaches it as
 * `VoiceConfig.Companion.BrainConfig` through a type alias.
 */
data class BrainConfig(
    /** [BRAIN_HUB], [BRAIN_LOCAL] or [BRAIN_CODEFLYING]. */
    val mode: String,
    val apiKey: String,        // empty = on-device brain cannot run
    val baseUrl: String,
    val model: String,
    val thinking: Boolean,
    val reasoningEffort: String,
    /**
     * Which provider [baseUrl] belongs to (the AI Provider Center's id). Blank on settings written before
     * providers had a name: the client then works it out from the URL, which is exactly what the app did
     * when DeepSeek was the only option.
     */
    val providerId: String = "",
    /** Optional generation parameters; null means "the provider's default", which is not the same as 0. */
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
    /** How long one request may take, and how many transient failures are retried on the same endpoint. */
    val readTimeoutSeconds: Long = 180,
    val maxRetries: Int = 1,
    /**
     * The endpoint to fall back to when the primary fails in a way another provider can route around. Null
     * on a device that configured only one provider, which is the common case.
     */
    val fallback: net.kuafuai.andee.brain.provider.ProviderEndpoint? = null,
) {
    /**
     * The agent loop runs on this device (local DeepSeek or CodeFlying
     * — both are "the brain is here", only the backend differs). Only
     * [BRAIN_HUB] is "run elsewhere, listen on a socket".
     */
    val isLocal: Boolean get() = mode == BRAIN_LOCAL || mode == BRAIN_CODEFLYING

    /**
     * Same backend, possibly asked differently. [mode], [apiKey] and
     * [baseUrl] say *who* answers; [model], [thinking] and
     * [reasoningEffort] only say how they are asked, and a running brain
     * can take those on the next request. A config change that is only
     * the second kind must not rebuild the brain, because rebuilding it
     * is what discards the conversation.
     */
    fun sameBackend(other: BrainConfig): Boolean =
        mode == other.mode && apiKey == other.apiKey && baseUrl == other.baseUrl &&
            providerId == other.providerId &&
            (fallback == null) == (other.fallback == null) &&
            (fallback == null || fallback.sameBackend(other.fallback!!))

    companion object {
        const val BRAIN_HUB = "hub"
        const val BRAIN_LOCAL = "local"
        const val BRAIN_CODEFLYING = "codeflying"
    }
}
