package com.dynamicislandpro.app.ui.preview

import android.view.View
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.ui.Ui

class PreviewPage(private val activity: MainActivity) {

    private val box = PreviewBox(activity, heightDp = 240)

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    fun refresh() = box.refresh()

    private fun buildContent(): View {
        val c = activity
        val root = Ui.page(c)
        root.addView(Ui.title(c, "Live Preview"))
        root.addView(box)

        val card = Ui.card(c)
        card.addView(Ui.heading(c, "Contoh aktivitas"))
        listOf("🔔 Notifikasi", "🎵 Musik", "⚡ Charging", "🔊 Volume", "⏱ Timer").forEachIndexed { i, label ->
            card.addView(Ui.button(c, label) { box.select(i) })
        }
        card.addView(Ui.button(c, "Perluas / ciutkan") { box.toggleExpanded() })
        root.addView(card)

        val help = Ui.card(c)
        help.addView(Ui.heading(c, "Gestur"))
        help.addView(Ui.body(c,
            "• Ketuk: perluas / ciutkan\n" +
                "• Tekan lama: buka kontrol\n" +
                "• Geser kiri/kanan: pindah aktivitas\n" +
                "• Geser ke atas: ciutkan\n" +
                "• Ketuk di luar Island: tutup tampilan expanded\n\n" +
                "Preview memakai tampilan yang sama dengan overlay asli dan mengikuti pengaturan di halaman Tampilan."))
        root.addView(help)
        return root
    }
}
