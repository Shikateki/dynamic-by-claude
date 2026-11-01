package com.dynamicislandpro.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.R
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandController
import com.dynamicislandpro.app.features.FeatureHost
import com.dynamicislandpro.app.utils.PreferencesManager

/**
 * Satu-satunya service yang berjalan terus-menerus. Memegang overlay dan fitur
 * yang dinyalakan. NotificationListener & Accessibility dikelola sistem sendiri.
 */
class IslandService : LifecycleService() {

    private var controller: IslandController? = null
    private var host: FeatureHost? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startAsForeground()
        controller = IslandController(this, lifecycleScope).also { it.start() }
        host = FeatureHost(this, lifecycleScope).also { it.start() }
        DynamicIslandEngine.serviceRunning.value = true
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            PreferencesManager.get(this).islandDesired = false
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        host?.stop()
        controller?.release()
        DynamicIslandEngine.clearAll()
        DynamicIslandEngine.serviceRunning.value = false
        host = null
        controller = null
        Log.i(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SERVICE, getString(R.string.channel_service), NotificationManager.IMPORTANCE_MIN)
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, getString(R.string.channel_alert), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun startAsForeground() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, IslandService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    companion object {
        private const val TAG = "IslandService"
        const val CHANNEL_SERVICE = "island_service"
        const val CHANNEL_ALERT = "island_alert"
        const val ACTION_STOP = "com.dynamicislandpro.app.STOP"
        private const val NOTIF_ID = 1001

        /** Aman dipanggil dari mana saja; tidak crash bila sistem menolak. */
        fun start(context: Context): Boolean = try {
            PreferencesManager.get(context).islandDesired = true
            ContextCompat.startForegroundService(context, Intent(context, IslandService::class.java))
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Cannot start service", t)
            false
        }

        fun stop(context: Context) {
            PreferencesManager.get(context).islandDesired = false
            context.stopService(Intent(context, IslandService::class.java))
        }
    }
}
