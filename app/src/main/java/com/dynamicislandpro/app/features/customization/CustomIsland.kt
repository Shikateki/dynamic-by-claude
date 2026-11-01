package com.dynamicislandpro.app.features.customization

import android.content.Context
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.timer.ReminderScheduler

/** Aktivitas buatan pengguna: teks/ikon kustom dan pengingat terjadwal. */
object CustomIsland {

    fun show(text: String, glyph: String, seconds: Int) {
        val clean = text.trim().ifEmpty { return }
        val icon = glyph.trim().ifEmpty { "✨" }
        DynamicIslandEngine.post(
            IslandActivity(
                id = "custom", type = IslandType.CUSTOM, priority = 55,
                title = clean, glyph = icon, timeoutMs = seconds.coerceIn(2, 60) * 1000L
            )
        )
    }

    fun remindIn(context: Context, minutes: Int, text: String) {
        val clean = text.trim().ifEmpty { "Pengingat" }
        val now = System.currentTimeMillis()
        val at = now + minutes.coerceAtLeast(1) * 60_000L
        // requestCode berbeda per pengingat supaya tidak saling menimpa.
        ReminderScheduler.schedule(
            context, 100 + (now % 10_000).toInt(), at,
            ReminderScheduler.KIND_REMINDER, "Pengingat", clean, now
        )
    }
}
