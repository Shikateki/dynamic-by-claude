package com.dynamicislandpro.app.features.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.R
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.services.IslandService

/**
 * Jadwal tunggal berbasis AlarmManager.setAndAllowWhileIdle (tidak butuh izin
 * alarm tepat). Dipakai untuk timer selesai dan pengingat. Waktu bisa meleset
 * beberapa menit saat Doze; saat service aktif, timer selesai tepat waktu lewat coroutine.
 */
object ReminderScheduler {

    const val KIND_TIMER = "timer"
    const val KIND_REMINDER = "reminder"
    private const val TAG = "ReminderScheduler"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TEXT = "text"
    private const val EXTRA_TOKEN = "token"

    private fun pending(context: Context, code: Int, intent: Intent?): PendingIntent? {
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (intent != null) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        val base = intent ?: Intent(context, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(context, code, base, flags)
    }

    fun schedule(context: Context, code: Int, atWallMs: Long, kind: String, title: String, text: String, token: Long) {
        val ctx = context.applicationContext
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .putExtra(EXTRA_KIND, kind).putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text).putExtra(EXTRA_TOKEN, token)
        val pi = pending(ctx, code, intent) ?: return
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atWallMs, pi)
        } catch (t: Throwable) {
            Log.w(TAG, "schedule failed", t)
        }
    }

    fun cancel(context: Context, code: Int) {
        val ctx = context.applicationContext
        try {
            val pi = pending(ctx, code, null) ?: return
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
            pi.cancel()
        } catch (t: Throwable) {
            Log.w(TAG, "cancel failed", t)
        }
    }

    /** Tampilkan di Island (bila aktif) dan sebagai notifikasi sistem (bila diizinkan). */
    fun announce(context: Context, id: String, title: String, text: String) {
        val ctx = context.applicationContext
        DynamicIslandEngine.post(
            IslandActivity(
                id = id, type = IslandType.REMINDER, priority = 90,
                title = title, subtitle = text, glyph = "⏰", timeoutMs = 10_000L
            )
        )
        try {
            val nm = NotificationManagerCompat.from(ctx)
            if (!nm.areNotificationsEnabled()) return
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(ctx, IslandService.CHANNEL_ALERT)
                .setSmallIcon(R.drawable.ic_stat_island)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
            @Suppress("MissingPermission")
            nm.notify(id.hashCode(), n)
        } catch (t: SecurityException) {
            Log.w(TAG, "notify denied", t)
        }
    }

    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent ?: return
            val title = intent.getStringExtra(EXTRA_TITLE) ?: return
            val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
            when (intent.getStringExtra(EXTRA_KIND)) {
                KIND_TIMER -> if (TimerHub.finish(intent.getLongExtra(EXTRA_TOKEN, 0L))) {
                    announce(context, "timer_done", title, text)
                }
                else -> announce(context, "reminder:${intent.getLongExtra(EXTRA_TOKEN, 0L)}", title, text)
            }
        }
    }
}
