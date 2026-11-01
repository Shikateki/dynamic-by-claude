package com.dynamicislandpro.app.ui.settings

import android.view.View
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.ui.Ui
import com.dynamicislandpro.app.ui.inputField
import com.dynamicislandpro.app.ui.toast

class FeaturesPage(private val activity: MainActivity) {

    private val prefs = activity.prefs

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    private fun parseList(raw: String): Set<String> =
        raw.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun buildContent(): View {
        val c = activity
        val s = prefs.settings.value
        val root = Ui.page(c)
        root.addView(Ui.title(c, "Pengaturan Fitur"))

        val features = Ui.card(c)
        features.addView(Ui.heading(c, "Fitur Island"))
        features.addView(Ui.switchRow(c, "🔔 Notifikasi", s.notificationsEnabled) { v -> prefs.update { it.copy(notificationsEnabled = v) } })
        features.addView(Ui.switchRow(c, "🎵 Musik", s.musicEnabled) { v -> prefs.update { it.copy(musicEnabled = v) } })
        features.addView(Ui.switchRow(c, "⚡ Baterai & charging", s.chargingEnabled) { v -> prefs.update { it.copy(chargingEnabled = v) } })
        features.addView(Ui.switchRow(c, "🔊 Volume", s.volumeEnabled) { v -> prefs.update { it.copy(volumeEnabled = v) } })
        features.addView(Ui.switchRow(c, "📶 Konektivitas", s.connectivityEnabled) { v -> prefs.update { it.copy(connectivityEnabled = v) } })
        features.addView(Ui.switchRow(c, "⏱ Timer & stopwatch", s.timerEnabled) { v -> prefs.update { it.copy(timerEnabled = v) } })
        features.addView(Ui.switchRow(c, "Getar saat tekan lama", s.hapticEnabled) { v -> prefs.update { it.copy(hapticEnabled = v) } })
        root.addView(features)

        val notif = Ui.card(c)
        notif.addView(Ui.heading(c, "Filter notifikasi"))
        notif.addView(Ui.sliderRow(c, "Lama tampil", 2, 30, 1, s.notificationTimeoutSec, " dtk") { v ->
            prefs.update { it.copy(notificationTimeoutSec = v) }
        })
        notif.addView(Ui.body(c, "\nWhitelist aplikasi (nama paket, pisahkan koma). Kosong = semua aplikasi."))
        val whitelist = inputField(c, "mis. com.whatsapp, org.telegram.messenger", initial = s.notificationApps.joinToString(", "))
        notif.addView(whitelist)
        notif.addView(Ui.button(c, "Simpan whitelist") {
            prefs.update { it.copy(notificationApps = parseList(whitelist.text.toString())) }
            c.toast("Whitelist disimpan")
        })
        root.addView(notif)

        val hidden = Ui.card(c)
        hidden.addView(Ui.heading(c, "Sembunyikan di aplikasi (Mode A)"))
        hidden.addView(Ui.body(c, "Island disembunyikan saat aplikasi ini di depan, mis. game atau pemutar video. Hanya bekerja bila Accessibility aktif; tanpa itu daftar diabaikan dan Island tetap tampil."))
        val hiddenInput = inputField(c, "mis. com.google.android.youtube", initial = s.hiddenApps.joinToString(", "))
        hidden.addView(hiddenInput)
        hidden.addView(Ui.button(c, "Simpan daftar") {
            prefs.update { it.copy(hiddenApps = parseList(hiddenInput.text.toString())) }
            c.toast("Daftar disimpan")
        })
        root.addView(hidden)
        return root
    }
}
