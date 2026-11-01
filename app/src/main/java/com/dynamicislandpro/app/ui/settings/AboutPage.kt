package com.dynamicislandpro.app.ui.settings

import android.view.View
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.ui.Ui

class AboutPage(private val activity: MainActivity) {

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    private fun buildContent(): View {
        val c = activity
        val version = try {
            c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "-"
        } catch (t: Exception) { "-" }

        val root = Ui.page(c)
        root.addView(Ui.title(c, "Tentang"))

        val about = Ui.card(c)
        about.addView(Ui.heading(c, "Dynamic Island Pro $version"))
        about.addView(Ui.body(c,
            "Dynamic Island native Android (Kotlin) untuk Redmi 12 · HyperOS 2 · Android 15. " +
                "Berjalan tanpa Accessibility (Mode B); Accessibility hanya menambah fitur sembunyikan-di-aplikasi (Mode A)."))
        root.addView(about)

        val privacy = Ui.card(c)
        privacy.addView(Ui.heading(c, "Privasi"))
        privacy.addView(Ui.body(c,
            "• Tidak ada internet, akun, atau pengiriman data. Semua diproses di perangkat.\n" +
                "• Isi notifikasi hanya ditampilkan di Island dan tidak disimpan.\n" +
                "• Accessibility hanya membaca nama paket aplikasi di depan; tidak membaca isi layar dan tidak melakukan klik."))
        root.addView(privacy)

        val limits = Ui.card(c)
        limits.addView(Ui.heading(c, "Batasan yang jujur"))
        limits.addView(Ui.body(c,
            "• Indikator screenshot, perekaman layar, clipboard, dan pengingat kalender belum ada: Android tidak menyediakan API resmi " +
                "yang bisa dipakai aplikasi biasa di latar belakang.\n" +
                "• Perubahan volume memakai broadcast sistem yang tidak terdokumentasi; bila HyperOS tidak mengirimnya, hanya fitur Volume yang tidak muncul.\n" +
                "• Nama Wi-Fi/perangkat Bluetooth tidak ditampilkan (butuh izin lokasi/Bluetooth).\n" +
                "• Timer saat Doze bisa meleset bila service dimatikan sistem."))
        root.addView(limits)
        return root
    }
}
