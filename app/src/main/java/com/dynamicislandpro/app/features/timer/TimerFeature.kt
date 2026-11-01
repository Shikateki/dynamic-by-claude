package com.dynamicislandpro.app.features.timer

import android.content.Context
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandAction
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.IslandFeature
import com.dynamicislandpro.app.utils.formatDuration
import com.dynamicislandpro.app.utils.formatElapsed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Menampilkan timer/stopwatch yang dikontrol dari UI. Teks live dibaca view, bukan diposting tiap detik. */
class TimerFeature(
    private val context: Context,
    private val scope: CoroutineScope
) : IslandFeature {

    private var observeJob: Job? = null
    private var finishJob: Job? = null

    override fun start() {
        observeJob = scope.launch {
            TimerHub.version.collect { refresh() }
        }
    }

    override fun stop() {
        observeJob?.cancel()
        finishJob?.cancel()
        observeJob = null
        finishJob = null
        DynamicIslandEngine.remove(ID)
    }

    private fun refresh() {
        finishJob?.cancel()
        finishJob = null
        val c = TimerHub.clock
        if (c.mode == TimerClock.Mode.IDLE) {
            DynamicIslandEngine.remove(ID)
            return
        }
        val countdown = c.mode == TimerClock.Mode.COUNTDOWN
        DynamicIslandEngine.post(
            IslandActivity(
                id = ID, type = IslandType.TIMER, priority = 65,
                title = if (countdown) "Timer" else "Stopwatch",
                subtitle = if (c.running) "Berjalan" else "Dijeda",
                glyph = "⏱",
                liveText = { if (countdown) formatDuration(c.remainingMs()) else formatElapsed(c.elapsedMs()) },
                liveProgress = { c.fraction() },
                actions = listOf(
                    IslandAction(if (c.running) "Jeda" else "Lanjut") {
                        if (c.running) TimerHub.pause(context) else TimerHub.resume(context)
                    },
                    IslandAction("Reset") { TimerHub.reset(context) }
                )
            )
        )
        if (countdown && c.running) {
            val token = TimerHub.token
            finishJob = scope.launch {
                delay(c.remainingMs())
                if (TimerHub.finish(token)) {
                    ReminderScheduler.cancel(context, 1)
                    ReminderScheduler.announce(context, "timer_done", "Timer selesai", "Waktu habis.")
                }
            }
        }
    }

    companion object {
        private const val ID = "timer"
    }
}
