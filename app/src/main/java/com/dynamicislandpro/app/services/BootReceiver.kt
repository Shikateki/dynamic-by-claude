package com.dynamicislandpro.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.dynamicislandpro.app.utils.PreferencesManager

/** Menyalakan kembali Island setelah boot, hanya jika pengguna memintanya & overlay diizinkan. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            val wanted = PreferencesManager.get(context).islandDesired
            if (wanted && Settings.canDrawOverlays(context)) {
                IslandService.start(context)
            }
        } catch (t: Throwable) {
            Log.w("BootReceiver", "Auto start failed", t)
        }
    }
}
