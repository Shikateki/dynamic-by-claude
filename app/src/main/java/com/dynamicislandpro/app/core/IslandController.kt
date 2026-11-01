package com.dynamicislandpro.app.core

import android.content.Context
import android.util.Log
import com.dynamicislandpro.app.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Menghubungkan Engine -> overlay. Membuat jendela hanya saat ada aktivitas
 * dan melepasnya (beserta view) saat Island kosong, supaya hemat RAM.
 */
class IslandController(context: Context, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val prefs = PreferencesManager.get(appContext)
    private val overlay = OverlayManager(appContext)
    private val animator = AnimationEngine { prefs.settings.value }

    private var root: IslandRoot? = null
    private var island: IslandView? = null
    private var hiding = false
    private val jobs = ArrayList<Job>()

    fun start() {
        jobs += scope.launch {
            combine(
                DynamicIslandEngine.state,
                DynamicIslandEngine.foregroundPackage,
                prefs.settings
            ) { state, fg, settings -> Triple(state, fg, settings) }
                .collect { (state, fg, settings) ->
                    val hidden = fg != null && fg in settings.hiddenApps
                    if (state.active == null || hidden) hide() else show(state, settings)
                }
        }
        jobs += scope.launch {
            prefs.settings.collect { s ->
                island?.let {
                    it.applyStyle()
                    it.relayout()
                    overlay.updatePosition(s)
                }
            }
        }
    }

    fun release() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        teardown()
    }

    private fun show(state: IslandUiState, settings: IslandSettings) {
        hiding = false
        var view = island
        if (view == null) {
            val v = IslandView(appContext, animator, settings = { prefs.settings.value })
            val r = IslandRoot(appContext)
            r.addView(v, v.initialLayoutParams())
            if (!overlay.attach(r, settings)) {
                Log.w(TAG, "Overlay not attached (permission?)")
                return
            }
            root = r
            island = v
            view = v
            view.bind(state)
            animator.appear(v)
            return
        }
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.bind(state)
    }

    private fun hide() {
        val v = island ?: return
        if (hiding) return
        hiding = true
        animator.disappear(v) {
            if (hiding) teardown()
        }
    }

    private fun teardown() {
        overlay.detach()
        animator.cancelAll()
        island?.resetShown()
        root?.removeAllViews()
        root = null
        island = null
        hiding = false
    }

    companion object {
        private const val TAG = "IslandController"
    }
}
