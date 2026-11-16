package net.kuafuai.andee.brain.provider

/**
 * How a provider's HTTP API is shaped. The three formats this app speaks today:
 *
 *  * [OPENAI] — the OpenAI chat-completions shape (OpenAI, DeepSeek, OpenRouter, Ollama, and every
 *    "OpenAI-compatible" gateway).
 *  * [GEMINI] — Google's generateContent shape. Not compatible with OpenAI: contents/parts, tools as
 *    functionDeclarations, no tool-call ids.
 *  * [ANTHROPIC] — Claude's messages shape. Not compatible either: system is a top-level field, tool
 *    results are content blocks inside a user message, and there is no tool_choice the same way.
 *
 * One adapter per format lives in [WireAdapters] (see [OpenAiWire], [GeminiWire], [AnthropicWire]).
 */
enum class WireFormat { OPENAI, GEMINI, ANTHROPIC }

/** One model a provider is known to serve. The id is what goes on the wire; the label is for the picker. */
data class ModelInfo(
    val id: String,
    val label: String = id,
    val tools: Boolean = true,
    val vision: Boolean = false,
    val reasoning: Boolean = false,
    val contextWindow: Int = 0,
)

/**
 * What a provider can actually do — the UI reads this so it only offers a switch the model will honour.
 * Capabilities are per-provider defaults; [ModelInfo] narrows them when a single model differs.
 */
data class ProviderCapabilities(
    val tools: Boolean = true,
    val vision: Boolean = false,
    val streaming: Boolean = false,
    val reasoning: Boolean = false,
    val temperature: Boolean = true,
    val topP: Boolean = true,
    val maxOutputTokens: Boolean = true,
    val customHeaders: Boolean = true,
    /** A local runtime (Ollama) that needs no key. */
    val local: Boolean = false,
    /** Model ids the provider lets the user type freely (OpenRouter, Ollama, custom gateways). */
    val freeModelIds: Boolean = false,
)

/**
 * A provider as the app knows it, independent of any key or model the user picked.
 *
 * Deliberately a value, not an interface with methods that call the network: the request/response shaping
 * is [WireAdapters]' job and the transport is [net.kuafuai.andee.brain.LlmClient]'s. Keeping the registry
 * inert is what lets it be tested without a server.
 */
data class AiProvider(
    val id: String,
    val label: String,
    val wire: WireFormat,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val models: List<ModelInfo> = emptyList(),
    val capabilities: ProviderCapabilities = ProviderCapabilities(),
    /** Shown under the key field so the user knows what to paste, e.g. "sk-…". */
    val keyHint: String = "",
    val docsUrl: String = "",
    /** One sentence of context for the settings card; empty when the name says enough. */
    val note: String = "",
) {
    fun model(id: String?): ModelInfo? =
        models.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }

    /** Whether a key must be typed before this provider can be saved. Ollama and friends say no. */
    val needsKey: Boolean get() = !capabilities.local
}

/**
 * The providers this build ships with. Ids are stored in the user's preferences, so they are a wire format
 * of their own: **never rename one** — add a new entry and keep the old id resolving, or every saved
 * provider silently turns into "Custom".
 */
object AiProviders {
    const val OPENAI = "openai"
    const val DEEPSEEK = "deepseek"
    const val GEMINI = "gemini"
    const val ANTHROPIC = "anthropic"
    const val OPENROUTER = "openrouter"
    const val OLLAMA = "ollama"
    const val CUSTOM = "custom"

    private val OPENAI_MODELS = listOf(
        ModelInfo("gpt-4o", "GPT-4o", vision = true, contextWindow = 128_000),
        ModelInfo("gpt-4o-mini", "GPT-4o mini", vision = true, contextWindow = 128_000),
        ModelInfo("gpt-4.1", "GPT-4.1", vision = true, contextWindow = 1_000_000),
        ModelInfo("gpt-4.1-mini", "GPT-4.1 mini", vision = true, contextWindow = 1_000_000),
        ModelInfo("o3-mini", "o3-mini (suy luận)", reasoning = true, contextWindow = 200_000),
    )

    private val DEEPSEEK_MODELS = listOf(
        ModelInfo("deepseek-chat", "DeepSeek Chat", contextWindow = 128_000),
        ModelInfo("deepseek-reasoner", "DeepSeek Reasoner (suy luận)", reasoning = true, contextWindow = 128_000),
    )

    private val GEMINI_MODELS = listOf(
        ModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", vision = true, reasoning = true, contextWindow = 1_048_576),
        ModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", vision = true, reasoning = true, contextWindow = 1_048_576),
        ModelInfo("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", vision = true, contextWindow = 1_048_576),
    )

    private val ANTHROPIC_MODELS = listOf(
        ModelInfo("claude-sonnet-4-5", "Claude Sonnet 4.5", vision = true, reasoning = true, contextWindow = 200_000),
        ModelInfo("claude-haiku-4-5", "Claude Haiku 4.5", vision = true, reasoning = true, contextWindow = 200_000),
        ModelInfo("claude-opus-4-1", "Claude Opus 4.1", vision = true, reasoning = true, contextWindow = 200_000),
    )

    val ALL: List<AiProvider> = listOf(
        AiProvider(
            id = DEEPSEEK,
            label = "DeepSeek",
            wire = WireFormat.OPENAI,
            defaultBaseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-chat",
            models = DEEPSEEK_MODELS,
            capabilities = ProviderCapabilities(reasoning = true, vision = false),
            keyHint = "sk-…",
            docsUrl = "https://platform.deepseek.com/api_keys",
            note = "Rẻ, tiếng Việt tốt, không nhìn được ảnh.",
        ),
        AiProvider(
            id = OPENAI,
            label = "OpenAI",
            wire = WireFormat.OPENAI,
            defaultBaseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            models = OPENAI_MODELS,
            capabilities = ProviderCapabilities(vision = true, reasoning = true, streaming = true),
            keyHint = "sk-…",
            docsUrl = "https://platform.openai.com/api-keys",
            note = "Nhìn được ảnh; cần thẻ quốc tế để nạp tiền.",
        ),
        AiProvider(
            id = GEMINI,
            label = "Google Gemini",
            wire = WireFormat.GEMINI,
            defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
            defaultModel = "gemini-2.5-flash",
            models = GEMINI_MODELS,
            capabilities = ProviderCapabilities(vision = true, reasoning = true, streaming = true),
            keyHint = "AIza…",
            docsUrl = "https://aistudio.google.com/apikey",
            note = "Có gói miễn phí hằng ngày; nhìn được ảnh.",
        ),
        AiProvider(
            id = ANTHROPIC,
            label = "Anthropic Claude",
            wire = WireFormat.ANTHROPIC,
            defaultBaseUrl = "https://api.anthropic.com",
            defaultModel = "claude-sonnet-4-5",
            models = ANTHROPIC_MODELS,
            capabilities = ProviderCapabilities(vision = true, reasoning = true, streaming = true),
            keyHint = "sk-ant-…",
            docsUrl = "https://console.anthropic.com/settings/keys",
            note = "Mạnh về viết và lập trình; trả lời bằng tiếng Việt tự nhiên.",
        ),
        AiProvider(
            id = OPENROUTER,
            label = "OpenRouter",
            wire = WireFormat.OPENAI,
            defaultBaseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "",
            models = emptyList(),
            capabilities = ProviderCapabilities(vision = true, reasoning = true, streaming = true, freeModelIds = true),
            keyHint = "sk-or-…",
            docsUrl = "https://openrouter.ai/keys",
            note = "Một key dùng được nhiều hãng; tự nhập mã model (vd: openai/gpt-4o-mini).",
        ),
        AiProvider(
            id = OLLAMA,
            label = "Ollama (máy chủ nội bộ)",
            wire = WireFormat.OPENAI,
            defaultBaseUrl = "http://127.0.0.1:11434/v1",
            defaultModel = "qwen3",
            models = listOf(
                ModelInfo("qwen3", "Qwen3"),
                ModelInfo("llama3.3", "Llama 3.3"),
                ModelInfo("deepseek-r1", "DeepSeek R1", reasoning = true),
                ModelInfo("gemma3", "Gemma 3", vision = true),
            ),
            capabilities = ProviderCapabilities(local = true, freeModelIds = true, streaming = true),
            keyHint = "Để trống",
            docsUrl = "https://ollama.com/download",
            note = "Chạy trên máy tính trong cùng mạng LAN; không cần API key, không tốn phí.",
        ),
        AiProvider(
            id = CUSTOM,
            label = "OpenAI-compatible (tự nhập URL)",
            wire = WireFormat.OPENAI,
            defaultBaseUrl = "",
            defaultModel = "",
            capabilities = ProviderCapabilities(vision = true, reasoning = true, streaming = true, freeModelIds = true),
            keyHint = "sk-… (nếu máy chủ yêu cầu)",
            note = "Bất kỳ máy chủ nào theo chuẩn /chat/completions: LM Studio, vLLM, proxy nội bộ…",
        ),
    )

    fun byId(id: String?): AiProvider? = ALL.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) }

    /**
     * Guess the provider from a base URL, for settings written before the provider had a name of its own.
     * Unknown URLs are [CUSTOM] — they behave exactly like an OpenAI-compatible server, which is what a
     * hand-typed URL almost always is.
     */
    fun match(baseUrl: String): AiProvider {
        val u = baseUrl.trim().lowercase()
        return when {
            u.isEmpty() -> byId(DEEPSEEK)!!
            "deepseek" in u -> byId(DEEPSEEK)!!
            "openrouter" in u -> byId(OPENROUTER)!!
            "generativelanguage.googleapis.com" in u || "gemini" in u -> byId(GEMINI)!!
            "anthropic" in u || "claude" in u -> byId(ANTHROPIC)!!
            "openai.com" in u -> byId(OPENAI)!!
            "127.0.0.1" in u || "localhost" in u || "ollama" in u || ":11434" in u -> byId(OLLAMA)!!
            else -> byId(CUSTOM)!!
        }
    }

    /** The provider for a saved pair; the id wins, the URL is the fallback for older settings. */
    fun providerFor(id: String?, baseUrl: String): AiProvider = byId(id) ?: match(baseUrl)
}
