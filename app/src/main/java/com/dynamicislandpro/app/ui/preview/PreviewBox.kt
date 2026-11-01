package com.dynamicislandpro.app.ui.preview

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import com.dynamicislandpro.app.core.AnimationEngine
import com.dynamicislandpro.app.core.IslandAction
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandInteraction
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.core.IslandUiState
import com.dynamicislandpro.app.core.IslandView
import com.dynamicislandpro.app.ui.dp
import com.dynamicislandpro.app.utils.PreferencesManager

/** Preview Island di dalam aplikasi. Memakai IslandView yang sama dengan overlay asli. */
class PreviewBox(context: Context, heightDp: Int = 190) : FrameLayout(context) {

    private val prefs = PreferencesManager.get(context)
    private val animator = AnimationEngine { prefs.settings.value }

    private var index = 0
    private var expanded = false
    private var controls = false

    private val noop = {}
    private val samples = listOf(
        IslandActivity(
            "p_notif", IslandType.NOTIFICATION, 70, "Pesan · Andi",
            "Halo, jadi ketemu jam 7 malam ini?", glyph = "💬",
            actions = listOf(IslandAction("Buka", noop), IslandAction("Hapus", noop))
        ),
        IslandActivity(
            "p_music", IslandType.MUSIC, 50, "Judul Lagu", "Nama Artis", glyph = "🎵", playing = true,
            liveProgress = { ((SystemClock.elapsedRealtime() / 100) % 1000) / 1000f },
            actions = listOf(IslandAction("⏮", noop), IslandAction("⏸", noop), IslandAction("⏭", noop))
        ),
        IslandActivity(
            "p_charge", IslandType.CHARGING, 60, "Mengisi daya", "Adaptor · 72%",
            trailing = "72%", glyph = "⚡", progress = 0.72f
        ),
        IslandActivity(
            "p_volume", IslandType.VOLUME, 40, "Volume media", "9 dari 15",
            trailing = "60%", glyph = "🔊", progress = 0.6f
        ),
        IslandActivity(
            "p_timer", IslandType.TIMER, 65, "Timer", "Berjalan", glyph = "⏱",
            liveText = { "04:59" }, liveProgress = { 0.5f },
            actions = listOf(IslandAction("Jeda", noop), IslandAction("Reset", noop))
        )
    )

    private val island = IslandView(context, animator, { prefs.settings.value }, object : IslandInteraction {
        override fun setExpanded(expanded: Boolean, withControls: Boolean) {
            this@PreviewBox.expanded = expanded
            controls = expanded && withControls
            push(0)
        }
        override fun next() { index = (index + 1) % samples.size; push(1) }
        override fun previous() { index = (index - 1 + samples.size) % samples.size; push(-1) }
        override fun dismiss(id: String) { expanded = false; controls = false; push(0) }
        override fun total(): Int = samples.size
    })

    init {
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(0xFF3A4A6B.toInt(), 0xFF1F2430.toInt())
        ).apply { cornerRadius = context.dp(16).toFloat() }
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, context.dp(heightDp)
        ).apply { bottomMargin = context.dp(12) }
        addView(island, island.initialLayoutParams().apply { topMargin = context.dp(12) })
        push(0)
    }

    fun select(i: Int) {
        index = i.coerceIn(0, samples.size - 1)
        push(1)
    }

    fun toggleExpanded() {
        expanded = !expanded
        controls = false
        push(0)
    }

    /** Dipanggil saat pengaturan tampilan berubah. */
    fun refresh() {
        island.applyStyle()
        island.relayout()
    }

    private fun push(direction: Int) {
        island.bind(IslandUiState(samples[index], samples.size, index, expanded, controls, direction))
    }
}
