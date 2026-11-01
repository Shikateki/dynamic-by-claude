package com.dynamicislandpro.app.core

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

/** Root jendela overlay. Menutup tampilan expanded saat pengguna menyentuh di luar. */
class IslandRoot(context: Context) : FrameLayout(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (DynamicIslandEngine.state.value.expanded) DynamicIslandEngine.setExpanded(false)
            return false
        }
        return super.dispatchTouchEvent(ev)
    }
}

/** Satu-satunya tempat yang menambah/menghapus jendela overlay (mencegah overlay ganda). */
class OverlayManager(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: View? = null

    val isAttached: Boolean get() = root != null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(appContext)

    fun attach(view: View, settings: IslandSettings): Boolean {
        if (root != null) return true
        if (!canDrawOverlays()) {
            Log.w(TAG, "Overlay permission missing; not attaching")
            return false
        }
        return try {
            windowManager.addView(view, buildParams(settings))
            root = view
            true
        } catch (t: Throwable) {
            Log.e(TAG, "addView failed", t)
            false
        }
    }

    fun detach() {
        val v = root ?: return
        root = null
        try {
            windowManager.removeViewImmediate(v)
        } catch (t: Throwable) {
            Log.w(TAG, "removeView failed", t)
        }
    }

    fun updatePosition(settings: IslandSettings) {
        val v = root ?: return
        try {
            windowManager.updateViewLayout(v, buildParams(settings))
        } catch (t: Throwable) {
            Log.w(TAG, "updateViewLayout failed", t)
        }
    }

    private fun buildParams(s: IslandSettings): WindowManager.LayoutParams {
        val d = appContext.resources.displayMetrics.density
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = (s.xOffsetDp * d).toInt()
            y = (s.yOffsetDp * d).toInt()
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            title = "DynamicIslandPro"
        }
    }

    companion object {
        private const val TAG = "OverlayManager"
    }
}
