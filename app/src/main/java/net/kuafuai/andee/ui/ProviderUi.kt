package net.kuafuai.andee.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import net.kuafuai.andee.R
import net.kuafuai.andee.brain.provider.AiProvider
import net.kuafuai.andee.brain.provider.AiProviders
import net.kuafuai.andee.brain.provider.ModelRole
import net.kuafuai.andee.compat.WindowMetricsCompat
import net.kuafuai.andee.config.ProviderStore
import net.kuafuai.andee.i18n.AppLocale
import net.kuafuai.andee.security.SecretBox

/**
 * The AI Provider Center: the one place where the user says which company (or which local server)
 * answers, with which key, on which model, and what to do when it fails.
 *
 * ### Why a card of its own
 *
 * Provider choice is a list, not a form: several records, one of them active, one fallback, and a model
 * per task. The settings card's save-on-close model (one value per row) cannot express that, so this card
 * owns its own writes — every button here saves immediately and tells the host so the brain can be
 * reconciled without closing the settings card first.
 *
 * ### What it deliberately does not do
 *
 * It never prints a stored key back into the field. A saved key shows as a masked placeholder; leaving the
 * field untouched on save keeps it, typing a new one replaces it, and the 清除 button removes it. The key
 * is only ever read to count its length or hand it to the HTTP layer — never logged.
 */
class ProviderUi(
    private val context: Context,
    private val onDismiss: () -> Unit = {},
    /** Called after every change so the host can reconcile the running brain with the new settings. */
    private val onChanged: () -> Unit = {},
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())
    private var root: View? = null
    private var frosted = false
    private var lctx: Context = context
    private var scroll: ScrollView? = null
    private var aside: Any? = null

    /** null = the list; otherwise the record being edited (a new one when [adding]). */
    private var editing: ProviderStore.Entry? = null
    private var adding = false
    private var roleEditing: ModelRole? = null

    /** Text fields currently on screen, by form key. */
    private val fields = mutableMapOf<String, EditText>()
    /** Segmented picks currently on screen, by form key. */
    private val picks = mutableMapOf<String, () -> String>()

    /** The live result of "test connection", drawn in the editor. */
    private var testText = ""
    private var testOk = false
    @Volatile private var testing = false

    // ---- window -------------------------------------------------------------------------------------

    fun show() {
        if (root != null) return
        val p = WindowManager.LayoutParams(
            cardWidth(), cardHeight(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable: the form has text fields, exactly like SettingsUi's.
            0,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        frosted = Glass.frost(context, wm, p)
        val v = build()
        runCatching {
            v.hideFromAccessibility()
            wm.addView(v, p)
            root = v
            OwnCard.shown()
            Glass.enter(v)
        }
        if (aside == null) aside = StandAside.register(wm) { root }
    }

    fun hide() {
        val v = root ?: return
        root = null
        aside?.let { StandAside.unregister(it) }; aside = null
        fields.clear(); picks.clear(); scroll = null
        Glass.exit(v) {
            runCatching { wm.removeView(v) }
            OwnCard.hidden()
            onDismiss()
        }
    }

    private fun rebuild() {
        val old = root ?: return
        // Carry what was typed into the editor across the rebuild. Without this a "test connection" or a
        // provider-type tap redraws the form from the saved record and silently throws away the key the
        // user just typed.
        snapshot()
        Glass.fadeOut(old) {
            if (root !== old) return@fadeOut
            fields.clear(); picks.clear()
            val v = build()
            runCatching {
                v.hideFromAccessibility()
                val p = old.layoutParams as WindowManager.LayoutParams
                wm.removeView(old); wm.addView(v, p); root = v
                Glass.fadeIn(v)
            }
        }
    }

    private fun cardWidth(): Int = minOf(dp(560), (WindowMetricsCompat.bounds(wm).width() * 0.96f).toInt())
    private fun cardHeight(): Int = minOf(dp(760), (WindowMetricsCompat.bounds(wm).height() * 0.92f).toInt())

    // ---- view ---------------------------------------------------------------------------------------

    @SuppressLint("SetTextI18n")
    private fun build(): View {
        lctx = AppLocale.wrap(context)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = Glass.card(context, dp(28), frosted)
            clipToOutline = true
        }
        card.addView(header())
        card.addView(View(context).apply { setBackgroundColor(Color.parseColor(Glass.HAIRLINE)) },
            LinearLayout.LayoutParams(MATCH, dp(1)))
        val sv = ScrollView(context).apply { isFillViewport = true; clipToPadding = false }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(20))
        }
        sv.addView(body, LinearLayout.LayoutParams(MATCH, WRAP))
        card.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))
        scroll = sv
        when {
            editing != null -> editorBody(body)
            roleEditing != null -> roleBody(body)
            else -> listBody(body)
        }
        return card
    }

    @SuppressLint("SetTextI18n")
    private fun header(): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(26), dp(20), dp(18), dp(14))
        }
        val titles = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(context).apply {
            text = lctx.getString(R.string.provider_center_title)
            textSize = Glass.Type.DISPLAY
            setTextColor(Color.parseColor(Glass.TITLE))
        })
        titles.addView(TextView(context).apply {
            text = lctx.getString(R.string.provider_center_subtitle)
            textSize = Glass.Type.CAPTION
            setTextColor(Color.parseColor(Glass.MUTED))
            setPadding(0, dp(4), 0, 0)
        })
        row.addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(TextView(context).apply {
            text = "✕"
            textSize = Glass.Type.BODY
            setTextColor(Color.parseColor(Glass.SECONDARY))
            gravity = Gravity.CENTER
            background = Glass.panel(context, dp(18))
            isClickable = true; isFocusable = true
            Glass.pressable(this)
            setOnClickListener { hide() }
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        return row
    }

    // ---- list ---------------------------------------------------------------------------------------

    private fun listBody(body: LinearLayout) {
        val entries = ProviderStore.entries(context)
        val activeId = ProviderStore.activeId(context)
        val fallbackId = ProviderStore.fallbackId(context)
        note(body, lctx.getString(R.string.provider_center_note))

        if (entries.isEmpty()) {
            note(body, lctx.getString(R.string.provider_empty))
        }
        for (e in entries) {
            val p = panel()
            val head = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            head.addView(TextView(context).apply {
                text = e.label.ifBlank { e.provider.label }
                textSize = Glass.Type.TITLE
                setTextColor(Color.parseColor(Glass.TITLE))
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            if (e.id == activeId) head.addView(badge(lctx.getString(R.string.provider_badge_active), Glass.ACCENT))
            else if (e.id == fallbackId) head.addView(badge(lctx.getString(R.string.provider_badge_fallback), Glass.WARN))
            if (!e.enabled) head.addView(badge(lctx.getString(R.string.provider_badge_off), Glass.MUTED))
            p.addView(head)
            line(p, e.provider.label + " · " + e.resolvedModel, Glass.LABEL, Glass.Type.BODY)
            line(p, e.resolvedBaseUrl, Glass.MUTED, Glass.Type.CAPTION)
            line(p,
                if (e.hasKey) lctx.getString(R.string.provider_key_saved) else if (e.provider.needsKey) lctx.getString(R.string.provider_key_missing) else lctx.getString(R.string.provider_key_not_needed),
                if (e.hasKey || !e.provider.needsKey) Glass.OK else Glass.WARN, Glass.Type.CAPTION)
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
            }
            actionRow.addView(smallButton(lctx.getString(R.string.provider_edit)) {
                adding = false; editing = e; testText = ""; rebuild()
            }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(6) })
            if (e.id != activeId) actionRow.addView(smallButton(lctx.getString(R.string.provider_use)) {
                ProviderStore.setActive(context, e.id); changed()
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            p.addView(actionRow)
            val actionRow2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, 0)
            }
            if (e.id != fallbackId && e.id != activeId) actionRow2.addView(smallButton(lctx.getString(R.string.provider_make_fallback)) {
                ProviderStore.setFallback(context, e.id); changed()
            }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(6) })
            actionRow2.addView(smallButton(if (e.enabled) lctx.getString(R.string.provider_disable) else lctx.getString(R.string.provider_enable)) {
                ProviderStore.setEnabled(context, e.id, !e.enabled); changed()
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
            p.addView(actionRow2)
            body.addView(p, rowParams(10))
        }

        body.addView(primaryButton(lctx.getString(R.string.provider_add)) {
            adding = true
            editing = newDraft()
            testText = ""
            rebuild()
        }, rowParams(14))

        rolesBody(body, entries)
    }

    /**
     * Which model covers each task. MAIN and FALLBACK are the two records above, so they are not repeated
     * here; the rest are roles the user can point somewhere cheaper or more capable.
     */
    private fun rolesBody(body: LinearLayout, entries: List<ProviderStore.Entry>) {
        body.addView(View(context), LinearLayout.LayoutParams(MATCH, dp(18)))
        body.addView(TextView(context).apply {
            text = lctx.getString(R.string.provider_roles_title)
            textSize = Glass.Type.HEADLINE
            setTextColor(Color.parseColor(Glass.TITLE))
        })
        note(body, lctx.getString(R.string.provider_roles_note))
        for (role in listOf(ModelRole.CODE, ModelRole.CHAT, ModelRole.VISION, ModelRole.LONG, ModelRole.CHEAP)) {
            val id = ProviderStore.roleEntryId(context, role)
            val chosen = ProviderStore.entry(context, id)
            val current = if (chosen != null) {
                chosen.label.ifBlank { chosen.provider.label } + " · " +
                    ProviderStore.roleModelOverride(context, role).ifBlank { chosen.resolvedModel }
            } else {
                lctx.getString(R.string.provider_roles_default)
            }
            val p = panel()
            line(p, roleLabel(role), Glass.LABEL, Glass.Type.BODY)
            line(p, current, Glass.MUTED, Glass.Type.CAPTION)
            p.addView(smallButton(lctx.getString(R.string.provider_choose)) {
                roleEditing = role; rebuild()
            }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(8) })
            body.addView(p, rowParams(8))
        }
    }

    private fun roleLabel(role: ModelRole): String = when (role) {
        ModelRole.CODE -> lctx.getString(R.string.provider_role_code)
        ModelRole.CHAT -> lctx.getString(R.string.provider_role_chat)
        ModelRole.VISION -> lctx.getString(R.string.provider_role_vision)
        ModelRole.LONG -> lctx.getString(R.string.provider_role_long)
        ModelRole.CHEAP -> lctx.getString(R.string.provider_role_cheap)
        ModelRole.MAIN -> lctx.getString(R.string.provider_role_main)
        ModelRole.FALLBACK -> lctx.getString(R.string.provider_role_fallback)
    }

    /** Choosing the record for one role. */
    private fun roleBody(body: LinearLayout) {
        val role = roleEditing ?: return
        body.addView(backRow())
        body.addView(TextView(context).apply {
            text = roleLabel(role)
            textSize = Glass.Type.HEADLINE
            setTextColor(Color.parseColor(Glass.TITLE))
            setPadding(0, dp(8), 0, dp(4))
        })
        note(body, lctx.getString(R.string.provider_role_pick_note))
        val entries = ProviderStore.entries(context)
        for (e in entries) {
            val p = panel()
            line(p, e.label.ifBlank { e.provider.label }, Glass.TITLE, Glass.Type.BODY)
            line(p, e.resolvedModel, Glass.MUTED, Glass.Type.CAPTION)
            p.isClickable = true
            Glass.pressable(p)
            p.setOnClickListener {
                ProviderStore.setRole(context, role, e.id, "")
                roleEditing = null
                changed()
            }
            body.addView(p, rowParams(8))
        }
        body.addView(smallButton(lctx.getString(R.string.provider_roles_clear)) {
            ProviderStore.setRole(context, role, "", "")
            roleEditing = null
            changed()
        }, rowParams(12))
    }

    // ---- editor -------------------------------------------------------------------------------------

    private fun editorBody(body: LinearLayout) {
        val e = editing ?: return
        val provider = ProviderProviders.current() ?: e.provider
        body.addView(backRow())
        body.addView(TextView(context).apply {
            text = if (adding) lctx.getString(R.string.provider_new_title) else lctx.getString(R.string.provider_edit_title)
            textSize = Glass.Type.HEADLINE
            setTextColor(Color.parseColor(Glass.TITLE))
            setPadding(0, dp(8), 0, dp(6))
        })

        // Provider type: a list rather than a segmented row, because seven names do not fit one line on a
        // phone and a clipped picker is worse than no picker.
        label(body, lctx.getString(R.string.provider_field_type))
        val list = panel()
        for (p in AiProviders.ALL) {
            val row = TextView(context).apply {
                text = p.label
                textSize = Glass.Type.BODY
                setTextColor(Color.parseColor(if (p.id == provider.id) Glass.ACCENT else Glass.LABEL))
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = if (p.id == provider.id) Glass.panel(context, dp(10)) else null
                isClickable = true
                Glass.pressable(this)
                setOnClickListener {
                    ProviderProviders.choose(p)
                    // Type changes reset the two fields that are provider-specific defaults, or the new
                    // provider would inherit the old one's URL and silently call the wrong service.
                    fields["base_url"]?.setText("")
                    fields["model"]?.setText("")
                    testText = ""
                    rebuild()
                }
            }
            list.addView(row)
        }
        body.addView(list, rowParams(4))

        field(body, "label", lctx.getString(R.string.provider_field_label),
            if (provider.id == AiProviders.CUSTOM) lctx.getString(R.string.provider_field_label_hint) else provider.label)
        field(body, "base_url", lctx.getString(R.string.provider_field_base_url),
            provider.defaultBaseUrl.ifEmpty { "https://…" })
        field(body, "model", lctx.getString(R.string.provider_field_model),
            provider.defaultModel.ifEmpty { "model-id" })
        if (provider.models.isNotEmpty()) {
            val chips = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            for (m in provider.models) {
                chips.addView(TextView(context).apply {
                    text = m.label + if (m.vision) " · " + lctx.getString(R.string.provider_vision_tag) else ""
                    textSize = Glass.Type.CAPTION
                    setTextColor(Color.parseColor(Glass.ACCENT))
                    setPadding(0, dp(3), 0, dp(3))
                    isClickable = true
                    Glass.pressable(this)
                    setOnClickListener {
                        fields["model"]?.setText(m.id)
                        testText = ""
                    }
                })
            }
            body.addView(chips, rowParams(2))
        }
        field(body, "api_key", lctx.getString(R.string.provider_field_key), provider.keyHint.ifEmpty { "sk-…" }, secret = true)
        if (e.hasKey) {
            note(body, lctx.getString(R.string.provider_key_keep_note))
            body.addView(smallButton(lctx.getString(R.string.provider_key_clear)) {
                fields["api_key"]?.setText("")
                ProviderStore.save(context, e, "")
                editing = ProviderStore.entry(context, e.id)
                testText = ""; changed()
            }, rowParams(6))
        }

        // Advanced parameters. Only the ones the provider actually supports are shown — see the class
        // KDoc on why a switch that does nothing is worse than no switch.
        body.addView(View(context), LinearLayout.LayoutParams(MATCH, dp(10)))
        label(body, lctx.getString(R.string.provider_advanced))
        if (provider.capabilities.reasoning) {
            label(body, lctx.getString(R.string.provider_field_thinking))
            segmented(body, "thinking", listOf("on" to common(R.string.common_on), "off" to common(R.string.common_off)))
            label(body, lctx.getString(R.string.provider_field_effort))
            segmented(body, "effort", listOf(
                "high" to common(R.string.common_high),
                "medium" to common(R.string.common_medium),
                "low" to common(R.string.common_low),
            ))
        }
        if (provider.capabilities.temperature) {
            field(body, "temperature", lctx.getString(R.string.provider_field_temperature), "0 – 2", numeric = true)
        }
        if (provider.capabilities.topP) {
            field(body, "top_p", lctx.getString(R.string.provider_field_top_p), "0 – 1", numeric = true)
        }
        if (provider.capabilities.maxOutputTokens) {
            field(body, "max_tokens", lctx.getString(R.string.provider_field_max_tokens), "vd: 4096", numeric = true)
        }
        field(body, "timeout", lctx.getString(R.string.provider_field_timeout), "180", numeric = true)
        field(body, "retries", lctx.getString(R.string.provider_field_retries), "1", numeric = true)

        if (testText.isNotEmpty()) {
            val tv = TextView(context).apply {
                text = testText
                textSize = Glass.Type.CAPTION
                setTextColor(Color.parseColor(if (testOk) Glass.OK else Glass.DANGER))
                setPadding(dp(4), dp(10), dp(4), dp(2))
            }
            body.addView(tv, rowParams(4))
        }

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, 0) }
        actions.addView(primaryButton(if (testing) lctx.getString(R.string.provider_testing) else lctx.getString(R.string.provider_test)) {
            runTest()
        }, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(8) })
        actions.addView(primaryButton(lctx.getString(R.string.provider_save)) { save() },
            LinearLayout.LayoutParams(0, WRAP, 1f))
        body.addView(actions, rowParams(8))
        if (!adding && e.id != ProviderStore.activeId(context)) {
            body.addView(smallButton(lctx.getString(R.string.provider_delete), danger = true) {
                ProviderStore.remove(context, e.id)
                editing = null
                changed()
            }, rowParams(12))
        }
    }

    // ---- actions ------------------------------------------------------------------------------------

    private fun save() {
        val e = editing ?: return
        val provider = ProviderProviders.current() ?: e.provider
        val label = fields["label"]?.text?.toString()?.trim().orEmpty()
            .ifEmpty { provider.label }
        val base = fields["base_url"]?.text?.toString()?.trim().orEmpty()
        val model = fields["model"]?.text?.toString()?.trim().orEmpty()
        val key = fields["api_key"]?.text?.toString()?.trim().orEmpty()
        if (provider.needsKey && key.isBlank() && !e.hasKey) {
            testText = lctx.getString(R.string.provider_err_need_key)
            testOk = false
            rebuild()
            return
        }
        if (base.isNotEmpty() && !base.startsWith("http")) {
            testText = lctx.getString(R.string.provider_err_url)
            testOk = false
            rebuild()
            return
        }
        val saved = e.copy(
            providerId = provider.id,
            label = label,
            baseUrl = base,
            model = model,
            thinking = picks["thinking"]?.invoke()?.let { it == "on" } ?: e.thinking,
            reasoningEffort = picks["effort"]?.invoke() ?: e.reasoningEffort,
            temperature = if (fields.containsKey("temperature")) num("temperature") else e.temperature,
            topP = if (fields.containsKey("top_p")) num("top_p") else e.topP,
            maxOutputTokens = if (fields.containsKey("max_tokens")) int("max_tokens") else e.maxOutputTokens,
            timeoutSeconds = int("timeout")?.toLong() ?: e.timeoutSeconds,
            maxRetries = int("retries") ?: e.maxRetries,
        )
        // A blank key on an existing record means "keep what is stored"; a typed one replaces it.
        ProviderStore.save(context, saved, if (key.isBlank()) null else key)
        editing = null
        adding = false
        testText = ""
        changed()
    }

    private fun runTest() {
        val e = editing ?: return
        val provider = ProviderProviders.current() ?: e.provider
        val key = fields["api_key"]?.text?.toString()?.trim().orEmpty().ifEmpty {
            ProviderStore.keyOf(e)
        }
        val probe = e.copy(
            providerId = provider.id,
            label = fields["label"]?.text?.toString()?.trim().orEmpty().ifEmpty { provider.label },
            baseUrl = fields["base_url"]?.text?.toString()?.trim().orEmpty(),
            model = fields["model"]?.text?.toString()?.trim().orEmpty(),
            thinking = false,
        )
        if (provider.needsKey && key.isBlank()) {
            testText = lctx.getString(R.string.provider_err_need_key)
            testOk = false
            rebuild()
            return
        }
        testing = true
        rebuild()
        Thread({
            val r = ProviderStore.test(probe, key)
            main.post {
                testing = false
                testOk = r.ok
                testText = if (r.ok) {
                    lctx.getString(R.string.provider_test_ok, r.latencyMs, probe.resolvedModel)
                } else {
                    lctx.getString(R.string.provider_test_fail, r.detail)
                }
                rebuild()
            }
        }, "provider-test").start()
    }

    /** Read the live form back into [editing] so a rebuild cannot lose what was typed. */
    private fun snapshot() {
        val e = editing ?: return
        if (fields.isEmpty()) return
        editing = e.copy(
            label = fields["label"]?.text?.toString()?.trim() ?: e.label,
            baseUrl = fields["base_url"]?.text?.toString()?.trim() ?: e.baseUrl,
            model = fields["model"]?.text?.toString()?.trim() ?: e.model,
            temperature = if (fields.containsKey("temperature")) num("temperature") else e.temperature,
            topP = if (fields.containsKey("top_p")) num("top_p") else e.topP,
            maxOutputTokens = if (fields.containsKey("max_tokens")) int("max_tokens") else e.maxOutputTokens,
            timeoutSeconds = int("timeout")?.toLong() ?: e.timeoutSeconds,
            maxRetries = int("retries") ?: e.maxRetries,
        )
    }

    private fun changed() {
        editing = null
        adding = false
        roleEditing = null
        testText = ""
        onChanged()
        rebuild()
    }

    private fun newDraft(): ProviderStore.Entry {
        ProviderProviders.reset()
        val p = AiProviders.ALL.first()
        return ProviderStore.Entry(
            id = "new_" + System.currentTimeMillis().toString(36),
            providerId = p.id,
            label = p.label,
            baseUrl = "",
            model = "",
            thinking = false,
            reasoningEffort = "high",
        )
    }

    // ---- small view helpers -------------------------------------------------------------------------

    private fun backRow(): View = TextView(context).apply {
        text = "‹ " + lctx.getString(R.string.provider_back)
        textSize = Glass.Type.CAPTION
        setTextColor(Color.parseColor(Glass.ACCENT))
        setPadding(0, 0, 0, dp(6))
        isClickable = true
        Glass.pressable(this)
        setOnClickListener {
            editing = null; adding = false; roleEditing = null; testText = ""
            rebuild()
        }
    }

    private fun panel(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = Glass.panel(context, dp(14))
        setPadding(dp(14), dp(12), dp(14), dp(12))
    }

    private fun line(parent: LinearLayout, text: String, color: String, size: Float): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(Color.parseColor(color))
            setPadding(0, dp(2), 0, dp(2))
        }.also { parent.addView(it) }

    private fun label(parent: LinearLayout, text: String) {
        parent.addView(TextView(context).apply {
            this.text = text
            textSize = Glass.Type.BODY
            setTextColor(Color.parseColor(Glass.LABEL))
            setPadding(0, dp(10), 0, dp(4))
        })
    }

    private fun note(parent: LinearLayout, text: String) {
        parent.addView(TextView(context).apply {
            this.text = text
            textSize = Glass.Type.CAPTION
            setTextColor(Color.parseColor(Glass.MUTED))
            setPadding(0, dp(2), 0, dp(4))
        })
    }

    private fun badge(text: String, color: String): TextView = TextView(context).apply {
        this.text = text
        textSize = Glass.Type.MICRO
        setTextColor(Color.parseColor(color))
        setPadding(dp(8), dp(2), dp(8), dp(2))
        background = Glass.panel(context, dp(9))
    }

    private fun field(parent: LinearLayout, key: String, labelText: String, hint: String, secret: Boolean = false, numeric: Boolean = false) {
        label(parent, labelText)
        val et = EditText(context).apply {
            this.hint = hint
            textSize = Glass.Type.BODY
            setTextColor(Color.parseColor(Glass.LABEL))
            setHintTextColor(Color.parseColor(Glass.MUTED))
            background = Glass.panel(context, dp(10))
            setPadding(dp(12), dp(9), dp(12), dp(9))
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            maxLines = 1
            setSingleLine(true)
        }
        if (secret) {
            et.setMasked(true)
            // A stored key is never printed back: the field stays empty with a hint saying one is stored.
            et.hint = if (editing?.hasKey == true) lctx.getString(R.string.provider_key_stored_hint) else hint
        }
        val value = when (key) {
            "label" -> editing?.label.orEmpty()
            "base_url" -> editing?.baseUrl.orEmpty()
            "model" -> editing?.model.orEmpty()
            "api_key" -> ""
            "temperature" -> editing?.temperature?.toString().orEmpty()
            "top_p" -> editing?.topP?.toString().orEmpty()
            "max_tokens" -> editing?.maxOutputTokens?.toString().orEmpty()
            "timeout" -> (editing?.timeoutSeconds ?: 180L).toString()
            "retries" -> (editing?.maxRetries ?: 1).toString()
            else -> ""
        }
        et.setText(value)
        fields[key] = et
        parent.addView(et, rowParams(4))
    }

    private fun segmented(parent: LinearLayout, key: String, options: List<Pair<String, String>>) {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(2), 0, dp(2)) }
        var selected = when (key) {
            "thinking" -> if (editing?.thinking == true) "on" else "off"
            "effort" -> editing?.reasoningEffort ?: "high"
            else -> options.firstOrNull()?.first ?: ""
        }
        val painted = ArrayList<Pair<String, TextView>>()
        fun repaint() {
            for ((value, tv) in painted) {
                val on = value == selected
                tv.setTextColor(Color.parseColor(if (on) Glass.ON_TINT else Glass.LABEL))
                tv.background = if (on) accentDrawable(dp(9)) else Glass.panel(context, dp(9))
            }
        }
        for ((value, text) in options) {
            val tv = TextView(context).apply {
                this.text = text
                textSize = Glass.Type.CAPTION
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                isClickable = true
                Glass.pressable(this)
                setOnClickListener {
                    selected = value
                    picks[key] = { selected }
                    repaint()
                }
            }
            painted += value to tv
            row.addView(tv, LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginEnd = dp(6) })
        }
        picks[key] = { selected }
        parent.addView(row)
    }

    /** A filled accent shape for the one primary action on screen. */
    private fun accentDrawable(radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.parseColor(Glass.ACCENT))
        cornerRadius = radius.toFloat()
    }

    private fun smallButton(text: String, danger: Boolean = false, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            textSize = Glass.Type.CAPTION
            setTextColor(Color.parseColor(if (danger) Glass.DANGER else Glass.SECONDARY))
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = Glass.panel(context, dp(9))
            isClickable = true; isFocusable = true
            Glass.pressable(this)
            setOnClickListener { onClick() }
        }

    private fun primaryButton(text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            textSize = Glass.Type.BODY
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Glass.ON_TINT))
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = accentDrawable(dp(12))
            isClickable = true; isFocusable = true
            Glass.pressable(this)
            setOnClickListener { onClick() }
        }

    // ---- numbers ------------------------------------------------------------------------------------

    private fun num(key: String): Double? = fields[key]?.text?.toString()?.trim()?.toDoubleOrNull()
    private fun int(key: String): Int? = fields[key]?.text?.toString()?.trim()?.toIntOrNull()

    private fun common(id: Int): String = lctx.getString(id)
    private fun dp(v: Int): Int = Glass.dp(context, v)
    private fun rowParams(top: Int) = LinearLayout.LayoutParams(MATCH, WRAP).apply { this.topMargin = dp(top) }

    /**
     * The provider type being edited. It is not in [picks] because it changes which rows the editor draws,
     * and rebuilding the editor is what the tap does anyway — so a tiny holder is simpler and cannot fall
     * out of step with the fields.
     */
    private object ProviderProviders {
        private var value: AiProvider? = null
        fun current(): AiProvider? = value
        fun choose(p: AiProvider) { value = p }
        fun reset() { value = null }
    }

    companion object {
        private const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
    }
}
