package com.dynamicislandpro.app.features.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.IslandFeature

/**
 * Indikator volume media / dering / alarm.
 *
 * Android tidak punya API publik untuk "volume berubah". Yang dipakai adalah
 * broadcast sistem VOLUME_CHANGED_ACTION (tidak terdokumentasi, tetapi dipakai luas).
 * Nilai volume dibaca lewat AudioManager resmi. Bila broadcast ini tidak dikirim
 * di HyperOS, hanya fitur Volume yang tidak muncul; fitur lain tidak terpengaruh.
 */
class VolumeFeature(private val context: Context) : IslandFeature {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var receiver: BroadcastReceiver? = null
    private val lastValue = HashMap<Int, Int>()

    override fun start() {
        // Isi nilai awal agar tidak menampilkan indikator saat fitur baru dinyalakan.
        for (s in STREAMS) lastValue[s] = audio.getStreamVolume(s)
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val type = intent?.getIntExtra(EXTRA_STREAM_TYPE, -1) ?: return
                if (type in STREAMS) show(type)
            }
        }
        receiver = r
        ContextCompat.registerReceiver(
            context, r, IntentFilter(ACTION_VOLUME_CHANGED), ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun stop() {
        receiver?.let {
            try { context.unregisterReceiver(it) } catch (t: IllegalArgumentException) { Log.w(TAG, "not registered") }
        }
        receiver = null
        lastValue.clear()
        DynamicIslandEngine.remove(ID)
    }

    private fun show(stream: Int) {
        val value = audio.getStreamVolume(stream)
        if (lastValue[stream] == value) return
        lastValue[stream] = value
        val max = audio.getStreamMaxVolume(stream).coerceAtLeast(1)
        val fraction = value / max.toFloat()
        val (name, glyph) = when (stream) {
            AudioManager.STREAM_RING -> "Volume dering" to (if (value == 0) "🔕" else "🔔")
            AudioManager.STREAM_ALARM -> "Volume alarm" to "⏰"
            else -> "Volume media" to (if (value == 0) "🔇" else if (fraction < 0.5f) "🔉" else "🔊")
        }
        DynamicIslandEngine.post(
            IslandActivity(
                id = ID, type = IslandType.VOLUME, priority = 40,
                title = name, subtitle = "$value dari $max",
                trailing = "${(fraction * 100).toInt()}%", glyph = glyph,
                progress = fraction, timeoutMs = 1500L
            )
        )
    }

    companion object {
        private const val TAG = "VolumeFeature"
        private const val ID = "volume"
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private val STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING, AudioManager.STREAM_ALARM
        )
    }
}
