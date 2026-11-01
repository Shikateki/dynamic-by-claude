package com.dynamicislandpro.app.ui.permissions

import android.os.Build
import android.view.View
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.ui.Ui

class PermissionPage(private val activity: MainActivity) {

    private val perms = activity.permissions

    private val overlay = Ui.StatusRow(
        activity, "Tampil di atas aplikasi lain", "SYSTEM_ALERT_WINDOW · wajib untuk menampilkan Island",
        "Buka"
    ) { activity.launchSettings(perms.overlayIntent()) }

    private val postNotif = Ui.StatusRow(
        activity, "Notifikasi aplikasi", "POST_NOTIFICATIONS · untuk notifikasi service, timer & pengingat",
        "Izinkan"
    ) {
        if (Build.VERSION.SDK_INT >= 33 && !perms.hasPostNotifications()) {
            activity.requestNotificationPermission { granted ->
                if (!granted) activity.launchSettings(perms.notificationSettingsIntent())
                refresh()
            }
        } else {
            activity.launchSettings(perms.notificationSettingsIntent())
        }
    }

    private val listener = Ui.StatusRow(
        activity, "Akses Notifikasi", "Notification Listener · untuk Notification Island & Music Island",
        "Buka"
    ) { activity.launchSettings(perms.notificationListenerIntent()) }

    private val accessibility = Ui.StatusRow(
        activity, "Accessibility (opsional)", "Hanya membaca nama aplikasi di depan. Tidak membaca isi layar.",
        "Buka"
    ) { activity.launchSettings(perms.accessibilityIntent()) }

    private val battery = Ui.StatusRow(
        activity, "Tanpa pembatasan baterai", "Agar HyperOS tidak mematikan Island di latar belakang",
        "Buka"
    ) { activity.launchSettings(perms.batteryIntent()) }

    private val autostart = Ui.StatusRow(
        activity, "Mulai otomatis (Autostart)", "Tidak bisa dideteksi aplikasi; atur manual di HyperOS",
        "Buka"
    ) { activity.launchSettings(perms.autostartIntent()) }

    private val boot = Ui.StatusRow(
        activity, "Boot & Foreground Service", "RECEIVE_BOOT_COMPLETED & FOREGROUND_SERVICE · izin normal", null, null
    )

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    private fun buildContent(): View {
        val c = activity
        val root = Ui.page(c)
        root.addView(Ui.title(c, "Pusat Izin"))
        root.addView(Ui.body(c, "Aplikasi tidak pernah memberi izin sendiri. Tombol hanya membuka halaman pengaturan Android."))

        val card = Ui.card(c).apply { setPadding(paddingLeft, paddingTop + 8, paddingRight, paddingBottom) }
        listOf(overlay, postNotif, listener, accessibility, battery, autostart, boot).forEach { card.addView(it.view) }
        root.addView(card)

        val guide = Ui.card(c)
        guide.addView(Ui.heading(c, "Panduan HyperOS 2 (Redmi 12)"))
        guide.addView(Ui.body(c, GUIDE))
        root.addView(guide)
        return root
    }

    fun refresh() {
        overlay.set(perms.hasOverlay(), if (perms.hasOverlay()) "Diizinkan" else "Belum diizinkan")
        postNotif.set(perms.hasPostNotifications(), if (perms.hasPostNotifications()) "Diizinkan" else "Belum diizinkan")
        listener.set(perms.hasNotificationListener(), if (perms.hasNotificationListener()) "Aktif" else "Belum aktif")
        val a11y = perms.hasAccessibility()
        accessibility.set(a11y, if (a11y) "Aktif" else "Tidak aktif (opsional)", neutral = !a11y)
        val bat = perms.isIgnoringBatteryOptimizations()
        battery.set(bat, if (bat) "Tanpa pembatasan" else "Masih dibatasi", neutral = !bat)
        autostart.set(false, "Periksa manual", neutral = true)
        boot.set(true, "Otomatis diberikan")
    }

    companion object {
        private const val GUIDE = """
1. Izin terbatas (PENTING). Karena APK dipasang di luar Play Store, Android 13+ menonaktifkan tombol Accessibility dan Akses Notifikasi. Buka Pengaturan › Aplikasi › Kelola aplikasi › Dynamic Island Pro, ketuk ⋮ di kanan atas, pilih "Izinkan pengaturan terbatas". Baru setelah itu aktifkan dua izin tersebut.

2. Mulai otomatis. Pengaturan › Aplikasi › Izin › Mulai otomatis › aktifkan Dynamic Island Pro.

3. Hemat baterai. Info aplikasi › Hemat baterai › pilih "Tanpa pembatasan".

4. Izin lain. Info aplikasi › Izin lainnya › aktifkan "Tampilkan jendela pop-up saat berjalan di latar belakang" dan "Tampilkan di layar kunci" jika tersedia.

5. Kunci di Recent Apps. Buka daftar aplikasi terbaru, tahan kartu Dynamic Island Pro, ketuk ikon gembok.

6. Jika Accessibility mati sendiri: buka aplikasi ini. Kartu peringatan muncul di Beranda. Island utama tetap jalan; aktifkan lagi Accessibility secara manual bila diperlukan. Aplikasi tidak akan me-restart-nya berulang kali.
"""
    }
}
