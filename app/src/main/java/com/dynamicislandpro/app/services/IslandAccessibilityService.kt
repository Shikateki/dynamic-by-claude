package com.dynamicislandpro.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.utils.PreferencesManager

/**
 * MODE A (opsional). Hanya membaca NAMA PAKET aplikasi di foreground dari event
 * perubahan jendela. Tidak membaca isi layar, tidak mengetik, tidak mengklik.
 * Lihat res/xml/accessibility_config.xml (canRetrieveWindowContent=false).
 */
class IslandAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        DynamicIslandEngine.accessibilityConnected.value = true
        PreferencesManager.get(this).accessibilityWasEnabled = true
        Log.i(TAG, "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg.contains("systemui") || pkg.contains("inputmethod")) return
        if (DynamicIslandEngine.foregroundPackage.value != pkg) {
            DynamicIslandEngine.foregroundPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        // Tidak ada umpan balik yang perlu dihentikan.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        DynamicIslandEngine.accessibilityConnected.value = false
        DynamicIslandEngine.foregroundPackage.value = null
        Log.i(TAG, "Accessibility disconnected")
        return super.onUnbind(intent)
    }

    companion object {
        private const val TAG = "IslandA11y"
    }
}
