package com.dynamicislandpro.app.utils

/** Format durasi: mm:ss, atau h:mm:ss bila lebih dari 1 jam. */
fun formatDuration(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) + 999L) / 1000L   // dibulatkan ke atas untuk countdown
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Untuk stopwatch: dibulatkan ke bawah agar tidak melompat lebih awal. */
fun formatElapsed(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000L
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
