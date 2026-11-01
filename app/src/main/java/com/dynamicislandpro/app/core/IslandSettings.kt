package com.dynamicislandpro.app.core

/**
 * Seluruh pengaturan yang bisa diubah pengguna. Immutable; ubah lewat
 * PreferencesManager.update { it.copy(...) }.
 */
data class IslandSettings(
    // Ukuran & posisi
    val widthDp: Int = 126,
    val heightDp: Int = 36,
    val xOffsetDp: Int = 0,
    val yOffsetDp: Int = 8,
    val cornerDp: Int = 24,
    val expandedWidthDp: Int = 340,
    val expandedHeightDp: Int = 120,
    // Animasi
    val animSpeed: Float = 1f,
    val animIntensity: Float = 1f,
    val animStyle: Int = STYLE_SPRING,
    // Warna
    val bgColor: Int = 0xFF000000.toInt(),
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val iconDp: Int = 20,
    val opacityPercent: Int = 100,
    // Fitur
    val notificationsEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val chargingEnabled: Boolean = true,
    val volumeEnabled: Boolean = true,
    val connectivityEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val notificationTimeoutSec: Int = 5,
    // Daftar package dipisah koma di UI; kosong = semua aplikasi
    val notificationApps: Set<String> = emptySet(),
    // Island disembunyikan saat aplikasi ini di foreground (butuh Accessibility)
    val hiddenApps: Set<String> = emptySet()
) {
    companion object {
        const val STYLE_SPRING = 0
        const val STYLE_SMOOTH = 1
        const val STYLE_ELASTIC = 2
    }
}
