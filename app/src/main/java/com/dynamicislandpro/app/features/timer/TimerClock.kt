package com.dynamicislandpro.app.features.timer

/**
 * Logika timer/stopwatch murni Kotlin (tanpa Android) agar bisa diuji unit test.
 * [now] adalah sumber waktu monotonic dalam milidetik.
 */
class TimerClock(private val now: () -> Long) {

    enum class Mode { IDLE, COUNTDOWN, STOPWATCH }

    var mode: Mode = Mode.IDLE
        private set
    var running: Boolean = false
        private set

    private var totalMs = 0L
    private var accumulated = 0L
    private var startedAt = 0L

    fun startCountdown(durationMs: Long) {
        require(durationMs > 0) { "duration must be positive" }
        mode = Mode.COUNTDOWN
        totalMs = durationMs
        accumulated = 0L
        startedAt = now()
        running = true
    }

    fun startStopwatch() {
        mode = Mode.STOPWATCH
        totalMs = 0L
        accumulated = 0L
        startedAt = now()
        running = true
    }

    fun pause() {
        if (!running) return
        accumulated += now() - startedAt
        running = false
    }

    fun resume() {
        if (running || mode == Mode.IDLE) return
        startedAt = now()
        running = true
    }

    fun reset() {
        mode = Mode.IDLE
        running = false
        totalMs = 0L
        accumulated = 0L
    }

    fun elapsedMs(): Long = accumulated + if (running) now() - startedAt else 0L

    fun remainingMs(): Long = (totalMs - elapsedMs()).coerceAtLeast(0L)

    fun isFinished(): Boolean = mode == Mode.COUNTDOWN && elapsedMs() >= totalMs

    /** Sisa waktu 0..1 untuk countdown; -1 bila tidak berlaku. */
    fun fraction(): Float =
        if (mode == Mode.COUNTDOWN && totalMs > 0) remainingMs().toFloat() / totalMs else -1f
}
