package com.dynamicislandpro.app.core

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dynamicislandpro.app.services.IslandAccessibilityService

/** Hanya membaca status & membuat Intent ke halaman Pengaturan. Tidak pernah memberi izin otomatis. */
class PermissionManager(context: Context) {

    private val ctx = context.applicationContext

    fun hasOverlay(): Boolean = Settings.canDrawOverlays(ctx)

    fun hasPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        }

    fun hasNotificationListener(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(ctx, IslandAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))

    fun notificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)

    fun notificationListenerIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun accessibilityIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun batteryIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Halaman Autostart Xiaomi/HyperOS; jika tidak ada, buka detail aplikasi. */
    fun autostartIntent(): Intent {
        val miui = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        return if (miui.resolveActivity(ctx.packageManager) != null) miui else appDetailsIntent()
    }

    fun appDetailsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
}
