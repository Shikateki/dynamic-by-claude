package com.dynamicislandpro.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.dynamicislandpro.app.core.IslandSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<IslandSettings> = _settings.asStateFlow()

    fun update(transform: (IslandSettings) -> IslandSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        save(next)
    }

    /** Pernah terlihat aktif? Dipakai untuk mendeteksi Accessibility yang dimatikan sistem. */
    var accessibilityWasEnabled: Boolean
        get() = prefs.getBoolean("a11y_was_enabled", false)
        set(value) { prefs.edit().putBoolean("a11y_was_enabled", value).apply() }

    /** Pengguna ingin Island menyala (dipakai ulang setelah boot). */
    var islandDesired: Boolean
        get() = prefs.getBoolean("island_desired", false)
        set(value) { prefs.edit().putBoolean("island_desired", value).apply() }

    private fun load(): IslandSettings {
        val d = IslandSettings()
        return IslandSettings(
            widthDp = prefs.getInt("widthDp", d.widthDp),
            heightDp = prefs.getInt("heightDp", d.heightDp),
            xOffsetDp = prefs.getInt("xOffsetDp", d.xOffsetDp),
            yOffsetDp = prefs.getInt("yOffsetDp", d.yOffsetDp),
            cornerDp = prefs.getInt("cornerDp", d.cornerDp),
            expandedWidthDp = prefs.getInt("expandedWidthDp", d.expandedWidthDp),
            expandedHeightDp = prefs.getInt("expandedHeightDp", d.expandedHeightDp),
            animSpeed = prefs.getFloat("animSpeed", d.animSpeed),
            animIntensity = prefs.getFloat("animIntensity", d.animIntensity),
            animStyle = prefs.getInt("animStyle", d.animStyle),
            bgColor = prefs.getInt("bgColor", d.bgColor),
            textColor = prefs.getInt("textColor", d.textColor),
            iconDp = prefs.getInt("iconDp", d.iconDp),
            opacityPercent = prefs.getInt("opacityPercent", d.opacityPercent),
            notificationsEnabled = prefs.getBoolean("notificationsEnabled", d.notificationsEnabled),
            musicEnabled = prefs.getBoolean("musicEnabled", d.musicEnabled),
            chargingEnabled = prefs.getBoolean("chargingEnabled", d.chargingEnabled),
            volumeEnabled = prefs.getBoolean("volumeEnabled", d.volumeEnabled),
            connectivityEnabled = prefs.getBoolean("connectivityEnabled", d.connectivityEnabled),
            timerEnabled = prefs.getBoolean("timerEnabled", d.timerEnabled),
            hapticEnabled = prefs.getBoolean("hapticEnabled", d.hapticEnabled),
            notificationTimeoutSec = prefs.getInt("notificationTimeoutSec", d.notificationTimeoutSec),
            notificationApps = prefs.getStringSet("notificationApps", emptySet())?.toSet() ?: emptySet(),
            hiddenApps = prefs.getStringSet("hiddenApps", emptySet())?.toSet() ?: emptySet()
        )
    }

    private fun save(s: IslandSettings) {
        prefs.edit()
            .putInt("widthDp", s.widthDp)
            .putInt("heightDp", s.heightDp)
            .putInt("xOffsetDp", s.xOffsetDp)
            .putInt("yOffsetDp", s.yOffsetDp)
            .putInt("cornerDp", s.cornerDp)
            .putInt("expandedWidthDp", s.expandedWidthDp)
            .putInt("expandedHeightDp", s.expandedHeightDp)
            .putFloat("animSpeed", s.animSpeed)
            .putFloat("animIntensity", s.animIntensity)
            .putInt("animStyle", s.animStyle)
            .putInt("bgColor", s.bgColor)
            .putInt("textColor", s.textColor)
            .putInt("iconDp", s.iconDp)
            .putInt("opacityPercent", s.opacityPercent)
            .putBoolean("notificationsEnabled", s.notificationsEnabled)
            .putBoolean("musicEnabled", s.musicEnabled)
            .putBoolean("chargingEnabled", s.chargingEnabled)
            .putBoolean("volumeEnabled", s.volumeEnabled)
            .putBoolean("connectivityEnabled", s.connectivityEnabled)
            .putBoolean("timerEnabled", s.timerEnabled)
            .putBoolean("hapticEnabled", s.hapticEnabled)
            .putInt("notificationTimeoutSec", s.notificationTimeoutSec)
            .putStringSet("notificationApps", HashSet(s.notificationApps))
            .putStringSet("hiddenApps", HashSet(s.hiddenApps))
            .apply()
    }

    companion object {
        @Volatile private var instance: PreferencesManager? = null

        fun get(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
    }
}
