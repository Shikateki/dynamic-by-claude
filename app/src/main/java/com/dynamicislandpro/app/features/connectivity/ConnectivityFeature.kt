package com.dynamicislandpro.app.features.connectivity

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.IslandFeature

/**
 * Wi-Fi / data seluler (ConnectivityManager), Bluetooth on/off dan mode pesawat
 * (broadcast resmi). Tidak membaca SSID atau nama perangkat (butuh izin lokasi/Bluetooth).
 */
class ConnectivityFeature(private val context: Context) : IslandFeature {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var receiver: BroadcastReceiver? = null
    private var lastKind: String? = null

    override fun start() {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val kind = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
                    else -> "other"
                }
                onKind(kind, caps.linkDownstreamBandwidthKbps)
            }

            override fun onLost(network: Network) {
                onKind("none", 0)
            }
        }
        callback = cb
        try {
            cm.registerDefaultNetworkCallback(cb)
        } catch (t: Throwable) {
            Log.w(TAG, "network callback failed", t)
            callback = null
        }

        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                            BluetoothAdapter.STATE_ON -> post("bt", "Bluetooth aktif", "", "🔷")
                            BluetoothAdapter.STATE_OFF -> post("bt", "Bluetooth mati", "", "▫️")
                        }
                    }
                    Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                        val on = intent.getBooleanExtra("state", isAirplaneOn())
                        post("airplane", if (on) "Mode pesawat aktif" else "Mode pesawat mati", "", "✈️")
                    }
                }
            }
        }
        receiver = r
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(context, r, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun stop() {
        callback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (t: IllegalArgumentException) { Log.w(TAG, "cb not registered") }
        }
        callback = null
        receiver?.let {
            try { context.unregisterReceiver(it) } catch (t: IllegalArgumentException) { Log.w(TAG, "rx not registered") }
        }
        receiver = null
        lastKind = null
        DynamicIslandEngine.removeByPrefix("conn:")
    }

    private fun isAirplaneOn(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    private fun onKind(kind: String, downKbps: Int) {
        val previous = lastKind
        lastKind = kind
        if (previous == null) return              // keadaan awal: jangan tampilkan
        if (previous == kind) return              // perubahan sinyal biasa: abaikan
        val speed = if (downKbps > 0) "Perkiraan ${downKbps / 1000} Mbps" else ""
        when (kind) {
            "wifi" -> post("net", "Wi-Fi terhubung", speed, "📶")
            "cell" -> post("net", "Data seluler", speed, "📡")
            "eth" -> post("net", "Ethernet terhubung", speed, "🔗")
            "none" -> post("net", "Tidak ada koneksi", "", "🚫")
            else -> post("net", "Jaringan terhubung", speed, "🌐")
        }
    }

    private fun post(key: String, title: String, subtitle: String, glyph: String) {
        DynamicIslandEngine.post(
            IslandActivity(
                id = "conn:$key", type = IslandType.CONNECTIVITY, priority = 30,
                title = title, subtitle = subtitle, glyph = glyph, timeoutMs = 3000L
            )
        )
    }

    companion object {
        private const val TAG = "ConnectivityFeature"
    }
}
