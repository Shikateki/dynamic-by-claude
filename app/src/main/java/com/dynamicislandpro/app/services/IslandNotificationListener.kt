package com.dynamicislandpro.app.services

import android.app.Notification
import android.app.NotificationManager
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandAction
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.utils.PreferencesManager

/** MODE B: notifikasi lewat API resmi NotificationListenerService (tanpa Accessibility). */
class IslandNotificationListener : NotificationListenerService() {

    private val counts = HashMap<String, Int>()
    private val lastPostAt = HashMap<String, Long>()
    private val latestKey = HashMap<String, String>()
    private val lastHash = HashMap<String, Int>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        DynamicIslandEngine.notificationAccessTick.value = DynamicIslandEngine.notificationAccessTick.value + 1
        Log.i(TAG, "Listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn == null) return
        try {
            handle(sbn, rankingMap)
        } catch (t: Throwable) {
            Log.w(TAG, "handle failed", t)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        if (sbn == null) return
        lastHash.remove(sbn.key)
        val pkg = sbn.packageName
        if (latestKey[pkg] == sbn.key) {
            latestKey.remove(pkg)
            counts.remove(pkg)
            DynamicIslandEngine.remove("notif:$pkg")
        }
    }

    private fun handle(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        val s = PreferencesManager.get(this).settings.value
        if (!s.notificationsEnabled || !DynamicIslandEngine.serviceRunning.value) return

        val pkg = sbn.packageName
        val n = sbn.notification
        if (pkg == packageName) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.category == Notification.CATEGORY_TRANSPORT || n.category == Notification.CATEGORY_PROGRESS) return
        if (n.visibility == Notification.VISIBILITY_SECRET) return
        if (s.notificationApps.isNotEmpty() && pkg !in s.notificationApps) return

        if (rankingMap != null) {
            val r = Ranking()
            if (rankingMap.getRanking(sbn.key, r) && r.importance < NotificationManager.IMPORTANCE_LOW) return
        }

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Hindari menampilkan notifikasi yang sama dua kali (update berulang).
        val hash = (title + "\u0000" + text).hashCode()
        if (lastHash[sbn.key] == hash) return
        lastHash[sbn.key] = hash

        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (t: Exception) { pkg }
        val icon = try { packageManager.getApplicationIcon(pkg) } catch (t: Exception) { null }

        // Grouping: satu entri per aplikasi dengan penghitung.
        val now = SystemClock.elapsedRealtime()
        val timeoutMs = s.notificationTimeoutSec.coerceIn(2, 30) * 1000L
        if (now - (lastPostAt[pkg] ?: 0L) > timeoutMs + 1000L) counts[pkg] = 0
        val count = (counts[pkg] ?: 0) + 1
        counts[pkg] = count
        lastPostAt[pkg] = now
        latestKey[pkg] = sbn.key

        val key = sbn.key
        val contentIntent = n.contentIntent
        val id = "notif:$pkg"
        DynamicIslandEngine.post(
            IslandActivity(
                id = id,
                type = IslandType.NOTIFICATION,
                priority = 70,
                title = if (title.isBlank()) appName else "$appName · $title",
                subtitle = text,
                trailing = if (count > 1) "×$count" else "",
                glyph = "🔔",
                icon = icon,
                timeoutMs = timeoutMs,
                actions = buildList {
                    if (contentIntent != null) add(IslandAction("Buka") {
                        try { contentIntent.send() } catch (t: Exception) { Log.w(TAG, "open failed", t) }
                        DynamicIslandEngine.remove(id)
                    })
                },
                onDismiss = { try { cancelNotification(key) } catch (t: Exception) { Log.w(TAG, "cancel failed", t) } }
            )
        )
    }

    companion object {
        private const val TAG = "IslandNotifListener"
    }
}
