package com.dynamicislandpro.app.features.charging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.IslandFeature

/**
 * Info baterai lewat ACTION_BATTERY_CHANGED (broadcast sticky resmi, tanpa polling).
 * Hanya memposting saat terjadi transisi: dicolok, dicabut, baterai lemah, penuh.
 */
class ChargingFeature(private val context: Context) : IslandFeature {

    private var receiver: BroadcastReceiver? = null
    private var lastCharging: Boolean? = null
    private var lowWarned = false
    private var fullNotified = false

    override fun start() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent != null) handle(intent, announce = true)
            }
        }
        receiver = r
        val sticky = ContextCompat.registerReceiver(
            context, r, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Keadaan awal: catat saja, jangan tampilkan.
        if (sticky != null) handle(sticky, announce = false)
    }

    override fun stop() {
        receiver?.let {
            try { context.unregisterReceiver(it) } catch (t: IllegalArgumentException) { Log.w(TAG, "not registered") }
        }
        receiver = null
        DynamicIslandEngine.remove(ID_CHARGING)
        DynamicIslandEngine.remove(ID_LOW)
        DynamicIslandEngine.remove(ID_FULL)
    }

    private fun handle(i: Intent, announce: Boolean) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val pct = level * 100 / scale
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val source = when (i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Adaptor"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
            else -> "-"
        }

        val previous = lastCharging
        lastCharging = charging
        if (announce && previous != null && previous != charging) {
            if (charging) {
                DynamicIslandEngine.post(
                    IslandActivity(
                        id = ID_CHARGING, type = IslandType.CHARGING, priority = 60,
                        title = "Mengisi daya", subtitle = "$source · $pct% · ${"%.1f".format(tempC)}°C",
                        trailing = "$pct%", glyph = "⚡", progress = pct / 100f, timeoutMs = 4000L
                    )
                )
            } else {
                DynamicIslandEngine.post(
                    IslandActivity(
                        id = ID_CHARGING, type = IslandType.CHARGING, priority = 60,
                        title = "Pengisi daya dicabut", subtitle = "Baterai $pct%",
                        trailing = "$pct%", glyph = "🔌", progress = pct / 100f, timeoutMs = 3000L
                    )
                )
            }
        }

        if (charging || pct > 20) lowWarned = false
        if (announce && !charging && pct <= LOW_THRESHOLD && !lowWarned) {
            lowWarned = true
            DynamicIslandEngine.post(
                IslandActivity(
                    id = ID_LOW, type = IslandType.CHARGING, priority = 85,
                    title = "Baterai lemah", subtitle = "Sisa $pct%. Colokkan pengisi daya.",
                    trailing = "$pct%", glyph = "🪫", progress = pct / 100f, timeoutMs = 6000L
                )
            )
        }

        if (status != BatteryManager.BATTERY_STATUS_FULL) fullNotified = false
        if (announce && status == BatteryManager.BATTERY_STATUS_FULL && !fullNotified) {
            fullNotified = true
            DynamicIslandEngine.post(
                IslandActivity(
                    id = ID_FULL, type = IslandType.CHARGING, priority = 62,
                    title = "Baterai penuh", subtitle = "Anda bisa mencabut pengisi daya.",
                    trailing = "100%", glyph = "🔋", progress = 1f, timeoutMs = 5000L
                )
            )
        }
    }

    companion object {
        private const val TAG = "ChargingFeature"
        private const val ID_CHARGING = "charging"
        private const val ID_LOW = "battery_low"
        private const val ID_FULL = "battery_full"
        private const val LOW_THRESHOLD = 15
    }
}
