package com.dynamicislandpro.app.core

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pusat pengelola aktivitas Island: antrean, prioritas, timeout, dan
 * aktivitas yang sedang tampil. Semua mutasi berjalan di main thread.
 */
object DynamicIslandEngine {

    private const val TAG = "IslandEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val items = LinkedHashMap<String, IslandActivity>()
    private val timeouts = HashMap<String, Job>()
    private var selectedId: String? = null
    private var expanded = false
    private var controls = false

    private val _state = MutableStateFlow(IslandUiState())
    val state: StateFlow<IslandUiState> = _state.asStateFlow()

    /** Status untuk Dashboard. */
    val serviceRunning = MutableStateFlow(false)
    val accessibilityConnected = MutableStateFlow(false)
    val foregroundPackage = MutableStateFlow<String?>(null)

    /** Bertambah setiap NotificationListener tersambung; fitur Music memakainya untuk mencoba ulang. */
    val notificationAccessTick = MutableStateFlow(0)

    fun post(activity: IslandActivity) {
        scope.launch { postInternal(activity) }
    }

    fun remove(id: String) {
        scope.launch {
            removeInternal(id)
            publish(0)
        }
    }

    fun removeByPrefix(prefix: String) {
        scope.launch {
            items.keys.filter { it.startsWith(prefix) }.forEach { removeInternal(it) }
            publish(0)
        }
    }

    /** Dismiss oleh pengguna: jalankan onDismiss (mis. batalkan notifikasi) lalu hapus. */
    fun dismiss(id: String) {
        scope.launch {
            try {
                items[id]?.onDismiss?.invoke()
            } catch (t: Throwable) {
                Log.w(TAG, "onDismiss failed for $id", t)
            }
            removeInternal(id)
            publish(0)
        }
    }

    fun next() = move(+1)
    fun previous() = move(-1)

    fun setExpanded(value: Boolean, withControls: Boolean = false) {
        scope.launch {
            if (items.isEmpty()) return@launch
            expanded = value
            controls = value && withControls
            publish(0)
        }
    }

    fun clearAll() {
        scope.launch {
            timeouts.values.forEach { it.cancel() }
            timeouts.clear()
            items.clear()
            selectedId = null
            publish(0)
        }
    }

    // ---- internal -------------------------------------------------------

    private fun postInternal(activity: IslandActivity) {
        val old = items[activity.id]
        val stored = if (old != null) activity.copy(createdAt = old.createdAt) else activity
        items[activity.id] = stored

        val current = items[selectedId] ?: sorted().firstOrNull()
        // Aktivitas baru hanya merebut layar bila tidak sedang expanded
        // dan prioritasnya tidak lebih rendah.
        if (old == null && !expanded && (current == null || stored.priority >= current.priority)) {
            selectedId = stored.id
        }
        scheduleTimeout(stored)
        publish(0)
    }

    private fun removeInternal(id: String) {
        items.remove(id)
        timeouts.remove(id)?.cancel()
        if (selectedId == id) selectedId = null
    }

    private fun scheduleTimeout(activity: IslandActivity) {
        timeouts.remove(activity.id)?.cancel()
        if (activity.timeoutMs <= 0L) return
        timeouts[activity.id] = scope.launch {
            delay(activity.timeoutMs)
            // Jangan menutup saat pengguna sedang membaca tampilan expanded.
            while (expanded && selectedId == activity.id) delay(1500)
            timeouts.remove(activity.id)
            removeInternal(activity.id)
            publish(0)
        }
    }

    private fun move(step: Int) {
        scope.launch {
            val list = sorted()
            if (list.size < 2) return@launch
            val idx = list.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            val target = (idx + step + list.size) % list.size
            selectedId = list[target].id
            publish(step)
        }
    }

    private fun sorted(): List<IslandActivity> =
        items.values.sortedWith(
            compareByDescending<IslandActivity> { it.priority }.thenByDescending { it.createdAt }
        )

    private fun publish(direction: Int) {
        val list = sorted()
        if (list.none { it.id == selectedId }) selectedId = list.firstOrNull()?.id
        val current = items[selectedId]
        if (current == null) {
            expanded = false
            controls = false
        }
        _state.value = IslandUiState(
            active = current,
            total = list.size,
            position = list.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
            expanded = expanded,
            controls = controls,
            direction = direction
        )
    }
}
