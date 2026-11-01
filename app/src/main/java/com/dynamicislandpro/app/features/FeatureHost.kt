package com.dynamicislandpro.app.features

import android.content.Context
import android.util.Log
import com.dynamicislandpro.app.features.charging.ChargingFeature
import com.dynamicislandpro.app.features.connectivity.ConnectivityFeature
import com.dynamicislandpro.app.features.music.MusicFeature
import com.dynamicislandpro.app.features.timer.TimerFeature
import com.dynamicislandpro.app.features.volume.VolumeFeature
import com.dynamicislandpro.app.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

interface IslandFeature {
    fun start()
    fun stop()
}

/** Menyalakan/mematikan setiap fitur sesuai pengaturan. Fitur yang mati tidak memakai resource. */
class FeatureHost(context: Context, private val scope: CoroutineScope) {

    private val appContext = context.applicationContext
    private val prefs = PreferencesManager.get(appContext)
    private val running = HashMap<String, IslandFeature>()
    private var job: Job? = null

    fun start() {
        job = scope.launch {
            prefs.settings
                .map { s ->
                    buildSet {
                        if (s.chargingEnabled) add("charging")
                        if (s.volumeEnabled) add("volume")
                        if (s.connectivityEnabled) add("connectivity")
                        if (s.musicEnabled) add("music")
                        if (s.timerEnabled) add("timer")
                    }
                }
                .distinctUntilChanged()
                .collect { sync(it) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        running.values.forEach { safeStop(it) }
        running.clear()
    }

    private fun sync(wanted: Set<String>) {
        running.keys.filter { it !in wanted }.forEach { key ->
            running.remove(key)?.let { safeStop(it) }
        }
        wanted.filter { it !in running }.forEach { key ->
            val feature = create(key)
            try {
                feature.start()
                running[key] = feature
            } catch (t: Throwable) {
                Log.e(TAG, "Feature $key failed to start", t)
                safeStop(feature)
            }
        }
    }

    private fun create(key: String): IslandFeature = when (key) {
        "charging" -> ChargingFeature(appContext)
        "volume" -> VolumeFeature(appContext)
        "connectivity" -> ConnectivityFeature(appContext)
        "music" -> MusicFeature(appContext, scope)
        else -> TimerFeature(appContext, scope)
    }

    private fun safeStop(f: IslandFeature) {
        try { f.stop() } catch (t: Throwable) { Log.w(TAG, "stop failed", t) }
    }

    companion object {
        private const val TAG = "FeatureHost"
    }
}
