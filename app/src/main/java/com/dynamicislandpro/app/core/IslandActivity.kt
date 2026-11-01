package com.dynamicislandpro.app.core

import android.graphics.drawable.Drawable
import android.os.SystemClock

enum class IslandType { CUSTOM, NOTIFICATION, MUSIC, CHARGING, VOLUME, CONNECTIVITY, TIMER, REMINDER }

data class IslandAction(val label: String, val onClick: () -> Unit)

/**
 * Satu "aktivitas" yang bisa ditampilkan Island. Aktivitas dengan id sama
 * saling menggantikan (mencegah tampilan ganda).
 *
 * liveText / liveProgress dibaca oleh IslandView (maksimal 2x per detik, hanya
 * saat terlihat) sehingga fitur tidak perlu memposting ulang setiap detik.
 */
data class IslandActivity(
    val id: String,
    val type: IslandType,
    val priority: Int,
    val title: String,
    val subtitle: String = "",
    val trailing: String = "",
    val glyph: String = "✨",
    val icon: Drawable? = null,
    val progress: Float = -1f,
    val timeoutMs: Long = 0L,
    val playing: Boolean = false,
    val actions: List<IslandAction> = emptyList(),
    val liveText: (() -> String)? = null,
    val liveProgress: (() -> Float)? = null,
    val onDismiss: (() -> Unit)? = null,
    val createdAt: Long = SystemClock.elapsedRealtime()
)

data class IslandUiState(
    val active: IslandActivity? = null,
    val total: Int = 0,
    val position: Int = 0,
    val expanded: Boolean = false,
    val controls: Boolean = false,
    /** +1 = berpindah ke berikutnya, -1 = sebelumnya, 0 = lainnya. Untuk arah slide. */
    val direction: Int = 0
)
