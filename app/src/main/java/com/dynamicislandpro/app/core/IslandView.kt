package com.dynamicislandpro.app.core

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Cara IslandView meneruskan gesture. Default: ke Engine; Preview punya implementasi sendiri. */
interface IslandInteraction {
    fun setExpanded(expanded: Boolean, withControls: Boolean)
    fun next()
    fun previous()
    fun dismiss(id: String)
    fun total(): Int
}

object EngineInteraction : IslandInteraction {
    override fun setExpanded(expanded: Boolean, withControls: Boolean) =
        DynamicIslandEngine.setExpanded(expanded, withControls)
    override fun next() = DynamicIslandEngine.next()
    override fun previous() = DynamicIslandEngine.previous()
    override fun dismiss(id: String) = DynamicIslandEngine.dismiss(id)
    override fun total(): Int = DynamicIslandEngine.state.value.total
}

/**
 * Pill Island. Ukuran dianimasikan lewat FrameLayout.LayoutParams sehingga
 * jendela overlay (WRAP_CONTENT) mengikuti tanpa layout melompat.
 */
class IslandView(
    context: Context,
    private val animator: AnimationEngine,
    private val settings: () -> IslandSettings,
    private val interaction: IslandInteraction = EngineInteraction
) : FrameLayout(context) {

    private val bg = GradientDrawable()
    private val content = LinearLayout(context)
    private val iconView = ImageView(context)
    private val titleText = TextView(context)
    private val subtitleText = TextView(context)
    private val trailingText = TextView(context)
    private val equalizer = EqualizerView(context)
    private val progress = ProgressLineView(context)
    private val actionsRow = LinearLayout(context)

    private var lastState = IslandUiState()
    private var shownId: String? = null
    private var expandedShown = false
    private var downX = 0f
    private var dragging = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun dpf(v: Float) = v * resources.displayMetrics.density

    private val ticker = object : Runnable {
        override fun run() {
            val a = lastState.active ?: return
            a.liveText?.let { f ->
                val t = safe { f() } ?: return@let
                if (trailingText.text != t) trailingText.text = t
            }
            a.liveProgress?.let { f -> safe { f() }?.let { progress.setProgress(it) } }
            if (a.liveText != null || a.liveProgress != null) postDelayed(this, 500L)
        }
    }

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            interaction.setExpanded(!lastState.expanded, false)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (settings().hapticEnabled) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            interaction.setExpanded(true, true)
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            val dx = e2.x - (e1?.x ?: e2.x)
            val dy = e2.y - (e1?.y ?: e2.y)
            return if (abs(dx) > abs(dy)) {
                if (abs(dx) > dp(40) || abs(vx) > 900f) {
                    if (dx < 0) interaction.next() else interaction.previous()
                    true
                } else false
            } else if (dy < -dp(24) && lastState.expanded) {
                interaction.setExpanded(false, false)
                true
            } else false
        }
    })

    init {
        background = bg

        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_VERTICAL
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL))

        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(iconView, LinearLayout.LayoutParams(dp(20), dp(20)))

        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleText.maxLines = 1
        titleText.ellipsize = android.text.TextUtils.TruncateAt.END
        subtitleText.maxLines = 3
        subtitleText.ellipsize = android.text.TextUtils.TruncateAt.END
        texts.addView(titleText)
        texts.addView(subtitleText)
        top.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(8)
            marginEnd = dp(8)
        })

        trailingText.maxLines = 1
        top.addView(trailingText)
        top.addView(equalizer, LinearLayout.LayoutParams(dp(18), dp(16)))
        content.addView(top, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        content.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(4)).apply {
            topMargin = dp(8)
        })

        actionsRow.orientation = LinearLayout.HORIZONTAL
        actionsRow.gravity = Gravity.CENTER
        content.addView(actionsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
        })

        content.setPadding(dp(14), dp(4), dp(14), dp(4))
        applyStyle()
    }

    fun initialLayoutParams(): LayoutParams {
        val s = settings()
        return LayoutParams(dp(s.widthDp), dp(s.heightDp), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    }

    /** Terapkan warna/ukuran ikon dari pengaturan. Aman dipanggil berulang. */
    fun applyStyle() {
        val s = settings()
        val alpha = (s.opacityPercent.coerceIn(20, 100) * 255 / 100)
        bg.setColor(ColorUtils.setAlphaComponent(s.bgColor, alpha))
        titleText.setTextColor(s.textColor)
        subtitleText.setTextColor(ColorUtils.setAlphaComponent(s.textColor, 0xB3))
        trailingText.setTextColor(s.textColor)
        progress.setColor(s.textColor)
        equalizer.setColor(s.textColor)
        iconView.layoutParams = (iconView.layoutParams as LinearLayout.LayoutParams).apply {
            width = dp(s.iconDp); height = dp(s.iconDp)
        }
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        trailingText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextSizes(expandedShown)
    }

    private fun setTextSizes(expanded: Boolean) {
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (expanded) 15f else 13f)
        titleText.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    fun bind(state: IslandUiState) {
        val act = state.active ?: return
        val previous = lastState
        lastState = state
        val changed = shownId != act.id
        val firstShow = shownId == null

        if (changed && !firstShow) {
            animator.swapContent(content, state.direction) { renderContent() }
        } else {
            renderContent()
        }
        shownId = act.id

        if (firstShow) {
            applySize(animate = false)
        } else if (previous.expanded != state.expanded || previous.controls != state.controls || changed) {
            applySize(animate = true)
        }

        removeCallbacks(ticker)
        if (act.liveText != null || act.liveProgress != null) post(ticker)
    }

    /** Hitung ulang ukuran tanpa animasi (dipakai saat pengaturan berubah). */
    fun relayout() = applySize(animate = false)

    fun resetShown() {
        shownId = null
        expandedShown = false
        removeCallbacks(ticker)
    }

    private fun renderContent() {
        val act = lastState.active ?: return
        val s = settings()
        val expanded = lastState.expanded

        iconView.setImageDrawable(act.icon ?: GlyphDrawable(act.glyph, dp(s.iconDp)))
        titleText.text = act.title
        subtitleText.text = act.subtitle
        subtitleText.visibility = if (act.subtitle.isNotEmpty() && expanded) VISIBLE else GONE
        titleText.maxLines = if (expanded) 2 else 1

        val isMusic = act.type == IslandType.MUSIC
        equalizer.visibility = if (isMusic) VISIBLE else GONE
        equalizer.setPlaying(isMusic && act.playing)
        trailingText.visibility = if (!isMusic && (act.trailing.isNotEmpty() || act.liveText != null)) VISIBLE else GONE
        trailingText.text = safe { act.liveText?.invoke() } ?: act.trailing

        val p = safe { act.liveProgress?.invoke() } ?: act.progress
        progress.setProgress(max(p, 0f))
        progress.visibility = if (p >= 0f && expanded) VISIBLE else GONE

        rebuildActions(act, expanded)
    }

    private fun rebuildActions(act: IslandActivity, expanded: Boolean) {
        actionsRow.removeAllViews()
        val list = ArrayList<IslandAction>(act.actions)
        if (lastState.controls) list.add(IslandAction("Tutup") { interaction.dismiss(act.id) })
        if (!expanded || list.isEmpty()) {
            actionsRow.visibility = GONE
            return
        }
        val tint = ColorUtils.setAlphaComponent(settings().textColor, 0x26)
        for (action in list) {
            val b = TextView(context).apply {
                text = action.label
                setTextColor(settings().textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dpf(16f)
                    setColor(tint)
                }
                setOnClickListener {
                    try {
                        action.onClick()
                    } catch (t: Throwable) {
                        Log.w(TAG, "action '${action.label}' failed", t)
                    }
                }
            }
            actionsRow.addView(b, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(4); marginEnd = dp(4) })
        }
        actionsRow.visibility = VISIBLE
    }

    private fun applySize(animate: Boolean) {
        val act = lastState.active ?: return
        val s = settings()
        val expanded = lastState.expanded
        val screenW = resources.displayMetrics.widthPixels

        val compactW = min(dp(s.widthDp + 60), screenW - dp(16))
        val compactH = dp(s.heightDp)
        val hasExtra = act.actions.isNotEmpty() || lastState.controls ||
            act.progress >= 0f || act.liveProgress != null || act.subtitle.isNotEmpty()
        var expH = dp(s.expandedHeightDp)
        if (!hasExtra) expH = min(expH, dp(72))
        if (lastState.controls) expH += dp(16)

        val targetW = if (expanded) min(dp(s.expandedWidthDp), screenW - dp(16)) else compactW
        val targetH = if (expanded) max(expH, compactH) else compactH
        val toRadius = if (expanded) min(dpf(s.cornerDp.toFloat()), targetH / 2f) else targetH / 2f
        val fromRadius = bg.cornerRadius

        val wasExpanded = expandedShown
        expandedShown = expanded
        setTextSizes(expanded)
        renderContent()

        val extras = listOf<View>(subtitleText, progress, actionsRow)
        val extrasVisible = extras.map { it.visibility == VISIBLE }

        if (!animate) {
            val lp = layoutParams as? LayoutParams ?: initialLayoutParams()
            lp.width = targetW
            lp.height = targetH
            layoutParams = lp
            bg.cornerRadius = toRadius
            extras.forEach { it.alpha = 1f }
            return
        }

        if (expanded && !wasExpanded) extras.forEachIndexed { i, v -> if (extrasVisible[i]) v.alpha = 0f }

        animator.resize(
            view = this, toW = targetW, toH = targetH, minW = compactH, minH = (compactH * 0.8f).toInt(),
            onProgress = { p ->
                bg.cornerRadius = max(0f, fromRadius + (toRadius - fromRadius) * p)
                val a = if (expanded) ((p - 0.3f) / 0.7f).coerceIn(0f, 1f) else 1f
                extras.forEachIndexed { i, v -> if (extrasVisible[i] && expanded) v.alpha = a }
            },
            onEnd = { extras.forEach { it.alpha = 1f } }
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = detector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                if (abs(dx) > dp(6)) dragging = true
                if (dragging && interaction.total() > 1) translationX = dx * 0.35f
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (translationX != 0f) animator.springBack(this)
            }
        }
        return handled || event.actionMasked == MotionEvent.ACTION_DOWN
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private inline fun <T> safe(block: () -> T?): T? =
        try { block() } catch (t: Throwable) { Log.w(TAG, "live provider failed", t); null }

    companion object {
        private const val TAG = "IslandView"
    }
}
