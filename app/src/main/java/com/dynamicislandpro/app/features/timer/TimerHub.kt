package com.dynamicislandpro.app.features.timer

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Satu-satunya pemilik status timer/stopwatch. UI mengubahnya lewat fungsi di sini;
 * TimerFeature (di service) mengamati [version] dan menampilkannya di Island.
 */
object TimerHub {

    val clock = TimerClock { SystemClock.elapsedRealtime() }

    /** Naik setiap status berubah. */
    val version = MutableStateFlow(0)

    /** Token unik tiap sesi countdown; mencegah penyelesaian ganda (coroutine vs alarm). */
    @Volatile var token: Long = 0L
        private set
    @Volatile private var completedToken: Long = -1L

    private const val ALARM_CODE = 1

    fun startCountdown(context: Context, durationMs: Long) {
        clock.startCountdown(durationMs)
        token = System.currentTimeMillis()
        ReminderScheduler.schedule(
            context, ALARM_CODE, System.currentTimeMillis() + durationMs,
            ReminderScheduler.KIND_TIMER, "Timer selesai", "Waktu habis.", token
        )
        bump()
    }

    fun startStopwatch() {
        clock.startStopwatch()
        bump()
    }

    fun pause(context: Context) {
        clock.pause()
        if (clock.mode == TimerClock.Mode.COUNTDOWN) ReminderScheduler.cancel(context, ALARM_CODE)
        bump()
    }

    fun resume(context: Context) {
        clock.resume()
        if (clock.mode == TimerClock.Mode.COUNTDOWN && clock.running) {
            ReminderScheduler.schedule(
                context, ALARM_CODE, System.currentTimeMillis() + clock.remainingMs(),
                ReminderScheduler.KIND_TIMER, "Timer selesai", "Waktu habis.", token
            )
        }
        bump()
    }

    fun reset(context: Context) {
        clock.reset()
        ReminderScheduler.cancel(context, ALARM_CODE)
        bump()
    }

    /**
     * Dipanggil saat countdown habis. Mengembalikan true hanya sekali per sesi,
     * sehingga notifikasi selesai tidak muncul dua kali.
     */
    fun finish(forToken: Long): Boolean {
        if (completedToken == forToken) return false
        completedToken = forToken
        if (clock.mode == TimerClock.Mode.COUNTDOWN && forToken == token) {
            clock.reset()
            bump()
        }
        return true
    }

    private fun bump() {
        version.value = version.value + 1
    }
}
