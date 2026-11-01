package com.dynamicislandpro.app.ui.dashboard

import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.core.DynamicIslandEngine
import com.dynamicislandpro.app.core.IslandAction
import com.dynamicislandpro.app.core.IslandActivity
import com.dynamicislandpro.app.core.IslandType
import com.dynamicislandpro.app.features.customization.CustomIsland
import com.dynamicislandpro.app.features.timer.TimerClock
import com.dynamicislandpro.app.features.timer.TimerHub
import com.dynamicislandpro.app.ui.Ui
import com.dynamicislandpro.app.ui.inputField
import com.dynamicislandpro.app.ui.toast
import com.google.android.material.button.MaterialButton

class DashboardPage(private val activity: MainActivity) {

    private val perms = activity.permissions
    private val prefs = activity.prefs

    private val islandRow = Ui.StatusRow(activity, "Island", "Overlay Dynamic Island", null, null)
    private val modeB = Ui.StatusRow(
        activity, "Mode B · Tanpa Accessibility",
        "Notifikasi, musik, baterai, volume, konektivitas, timer", null, null
    )
    private val modeA = Ui.StatusRow(
        activity, "Mode A · Accessibility (opsional)",
        "Menyembunyikan Island di aplikasi tertentu", null, null
    )
    private lateinit var warning: LinearLayout
    private lateinit var toggle: MaterialButton

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    private fun buildContent(): LinearLayout {
        val c = activity
        val root = Ui.page(c)
        root.addView(Ui.title(c, "Dynamic Island Pro"))

        warning = Ui.card(c).apply {
            addView(Ui.heading(c, "⚠️ Accessibility berhenti"))
            addView(Ui.body(c,
                "Accessibility Service sebelumnya aktif tetapi sekarang mati (HyperOS dapat menghentikannya " +
                    "saat hemat baterai). Island utama tetap berjalan. Aktifkan lagi bila ingin fitur sembunyikan-di-aplikasi."))
            addView(Ui.button(c, "Buka pengaturan Accessibility") { activity.launchSettings(perms.accessibilityIntent()) })
            addView(Ui.button(c, "Abaikan peringatan") { prefs.accessibilityWasEnabled = false; refresh() })
            visibility = View.GONE
        }
        root.addView(warning)

        val status = Ui.card(c)
        status.addView(Ui.heading(c, "Status"))
        status.addView(islandRow.view)
        status.addView(modeB.view)
        status.addView(modeA.view)
        toggle = Ui.button(c, "Nyalakan Island") { activity.toggleIsland() }
        status.addView(toggle)
        root.addView(status)

        root.addView(buildQuickTest())
        root.addView(buildTimerCard())
        root.addView(buildCustomCard())
        return root
    }

    private fun buildQuickTest(): View {
        val c = activity
        val card = Ui.card(c)
        card.addView(Ui.heading(c, "Tes cepat"))
        card.addView(Ui.body(c, "Menampilkan 3 aktivitas contoh sekaligus. Geser kiri/kanan di Island untuk berpindah."))
        card.addView(Ui.button(c, "Tampilkan contoh") {
            if (!requireRunning()) return@button
            DynamicIslandEngine.post(IslandActivity(
                "test_notif", IslandType.NOTIFICATION, 70, "Pesan · Andi", "Halo, jadi ketemu jam 7 malam?",
                glyph = "💬", timeoutMs = 20_000L,
                actions = listOf(IslandAction("Hapus") { DynamicIslandEngine.remove("test_notif") })
            ))
            DynamicIslandEngine.post(IslandActivity(
                "test_volume", IslandType.VOLUME, 40, "Volume media", "9 dari 15", trailing = "60%",
                glyph = "🔊", progress = 0.6f, timeoutMs = 20_000L
            ))
            DynamicIslandEngine.post(IslandActivity(
                "test_charge", IslandType.CHARGING, 60, "Mengisi daya", "Adaptor · 72%", trailing = "72%",
                glyph = "⚡", progress = 0.72f, timeoutMs = 20_000L
            ))
        })
        return card
    }

    private fun buildTimerCard(): View {
        val c = activity
        val card = Ui.card(c)
        card.addView(Ui.heading(c, "Timer & Stopwatch"))
        val minutes = inputField(c, "Menit", numeric = true, initial = "5")
        card.addView(minutes)
        card.addView(Ui.button(c, "Mulai timer") {
            if (!requireRunning()) return@button
            val m = minutes.text.toString().toLongOrNull()
            if (m == null || m <= 0L) c.toast("Isi menit lebih dari 0") else TimerHub.startCountdown(c, m * 60_000L)
        })
        card.addView(Ui.button(c, "Mulai stopwatch") {
            if (requireRunning()) TimerHub.startStopwatch()
        })
        card.addView(Ui.button(c, "Jeda / Lanjut") {
            val clock = TimerHub.clock
            if (clock.mode == TimerClock.Mode.IDLE) return@button
            if (clock.running) TimerHub.pause(c) else TimerHub.resume(c)
        })
        card.addView(Ui.button(c, "Reset") { TimerHub.reset(c) })
        return card
    }

    private fun buildCustomCard(): View {
        val c = activity
        val card = Ui.card(c)
        card.addView(Ui.heading(c, "Aktivitas kustom & pengingat"))
        val text = inputField(c, "Teks")
        val glyph = inputField(c, "Ikon (emoji)", initial = "✨")
        val minutes = inputField(c, "Pengingat dalam … menit", numeric = true, initial = "10")
        card.addView(text)
        card.addView(glyph)
        card.addView(Ui.button(c, "Tampilkan di Island") {
            if (!requireRunning()) return@button
            if (text.text.isBlank()) c.toast("Isi teks dulu") else CustomIsland.show(text.text.toString(), glyph.text.toString(), 6)
        })
        card.addView(minutes)
        card.addView(Ui.button(c, "Ingatkan saya") {
            val m = minutes.text.toString().toIntOrNull()
            if (m == null || m <= 0) {
                c.toast("Isi menit lebih dari 0")
            } else {
                CustomIsland.remindIn(c, m, text.text.toString())
                c.toast("Pengingat diatur $m menit lagi")
            }
        })
        return card
    }

    private fun requireRunning(): Boolean {
        if (DynamicIslandEngine.serviceRunning.value) return true
        activity.toast("Nyalakan Island dulu")
        return false
    }

    fun refresh() {
        val running = DynamicIslandEngine.serviceRunning.value
        val overlay = perms.hasOverlay()
        val listener = perms.hasNotificationListener()
        val a11y = perms.hasAccessibility()

        islandRow.set(
            running,
            when {
                running -> "Aktif"
                !overlay -> "Mati · izin overlay belum diberikan"
                else -> "Mati"
            }
        )
        modeB.set(
            running && listener,
            when {
                !running -> "Island belum menyala"
                listener -> "Aktif · notifikasi & musik tersambung"
                else -> "Aktif · notifikasi & musik butuh Akses Notifikasi"
            },
            neutral = running && !listener
        )
        modeA.set(
            a11y,
            if (a11y) "Aktif" else "Tidak aktif · fitur inti tetap berjalan",
            neutral = !a11y
        )
        warning.visibility = if (prefs.accessibilityWasEnabled && !a11y) View.VISIBLE else View.GONE
        toggle.text = if (running) "Matikan Island" else "Nyalakan Island"
    }
}
