package com.dynamicislandpro.app

import com.dynamicislandpro.app.features.timer.TimerClock
import com.dynamicislandpro.app.utils.formatDuration
import com.dynamicislandpro.app.utils.formatElapsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerClockTest {

    private var t = 0L
    private val clock = TimerClock { t }

    @Test fun countdownRunsAndFinishes() {
        clock.startCountdown(10_000)
        t += 4_000
        assertEquals(6_000, clock.remainingMs())
        assertFalse(clock.isFinished())
        t += 6_000
        assertTrue(clock.isFinished())
        assertEquals(0, clock.remainingMs())
    }

    @Test fun pauseStopsTimeAndResumeContinues() {
        clock.startCountdown(10_000)
        t += 3_000
        clock.pause()
        t += 50_000
        assertEquals(7_000, clock.remainingMs())
        clock.resume()
        t += 2_000
        assertEquals(5_000, clock.remainingMs())
    }

    @Test fun stopwatchCountsUpAndResets() {
        clock.startStopwatch()
        t += 90_000
        assertEquals(90_000, clock.elapsedMs())
        clock.reset()
        assertEquals(TimerClock.Mode.IDLE, clock.mode)
        assertEquals(0, clock.elapsedMs())
    }

    @Test fun formatting() {
        assertEquals("01:05", formatDuration(65_000))
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("00:01", formatDuration(1))          // dibulatkan ke atas
        assertEquals("00:00", formatElapsed(999))
        assertEquals("00:59", formatElapsed(59_999))
    }
}
