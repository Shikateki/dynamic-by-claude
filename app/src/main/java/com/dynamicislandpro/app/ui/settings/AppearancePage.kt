package com.dynamicislandpro.app.ui.settings

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import com.dynamicislandpro.app.MainActivity
import com.dynamicislandpro.app.core.IslandSettings
import com.dynamicislandpro.app.ui.Ui
import com.dynamicislandpro.app.ui.dp
import com.dynamicislandpro.app.ui.preview.PreviewBox

class AppearancePage(private val activity: MainActivity) {

    private val prefs = activity.prefs
    private val preview = PreviewBox(activity, heightDp = 200)

    val view: View = ScrollView(activity).apply { addView(buildContent()) }

    fun refresh() = preview.refresh()

    private fun buildContent(): View {
        val c = activity
        val s = prefs.settings.value
        val root = Ui.page(c)
        root.addView(Ui.title(c, "Tampilan"))
        root.addView(preview)
        root.addView(Ui.button(c, "Perluas / ciutkan preview") { preview.toggleExpanded() })

        val size = Ui.card(c)
        size.addView(Ui.heading(c, "Ukuran & posisi"))
        size.addView(Ui.sliderRow(c, "Lebar dasar", 80, 200, 2, s.widthDp, " dp") { v -> prefs.update { it.copy(widthDp = v) } })
        size.addView(Ui.sliderRow(c, "Tinggi", 28, 60, 1, s.heightDp, " dp") { v -> prefs.update { it.copy(heightDp = v) } })
        size.addView(Ui.sliderRow(c, "Posisi horizontal", -150, 150, 5, s.xOffsetDp, " dp") { v -> prefs.update { it.copy(xOffsetDp = v) } })
        size.addView(Ui.sliderRow(c, "Posisi vertikal", 0, 80, 1, s.yOffsetDp, " dp") { v -> prefs.update { it.copy(yOffsetDp = v) } })
        size.addView(Ui.sliderRow(c, "Radius sudut (expanded)", 8, 48, 1, s.cornerDp, " dp") { v -> prefs.update { it.copy(cornerDp = v) } })
        size.addView(Ui.sliderRow(c, "Lebar expanded", 240, 380, 10, s.expandedWidthDp, " dp") { v -> prefs.update { it.copy(expandedWidthDp = v) } })
        size.addView(Ui.sliderRow(c, "Tinggi expanded", 80, 200, 4, s.expandedHeightDp, " dp") { v -> prefs.update { it.copy(expandedHeightDp = v) } })
        root.addView(size)

        val look = Ui.card(c)
        look.addView(Ui.heading(c, "Warna & ikon"))
        look.addView(Ui.sliderRow(c, "Ukuran ikon", 14, 32, 1, s.iconDp, " dp") { v -> prefs.update { it.copy(iconDp = v) } })
        look.addView(Ui.sliderRow(c, "Transparansi (opasitas)", 30, 100, 5, s.opacityPercent, "%") { v -> prefs.update { it.copy(opacityPercent = v) } })
        look.addView(Ui.body(c, "\nWarna latar"))
        look.addView(swatches(listOf(0xFF000000, 0xFF1C1C1E, 0xFF0A1A3A, 0xFF06281B, 0xFF22103A)) { col ->
            prefs.update { it.copy(bgColor = col) }
        })
        look.addView(Ui.body(c, "\nWarna teks"))
        look.addView(swatches(listOf(0xFFFFFFFF, 0xFFD0D0D0, 0xFF30D158, 0xFF64D2FF, 0xFFFFD60A)) { col ->
            prefs.update { it.copy(textColor = col) }
        })
        root.addView(look)

        val anim = Ui.card(c)
        anim.addView(Ui.heading(c, "Animasi"))
        anim.addView(Ui.sliderRow(c, "Kecepatan", 50, 200, 5, (s.animSpeed * 100).toInt(), "%") { v ->
            prefs.update { it.copy(animSpeed = v / 100f) }
        })
        anim.addView(Ui.sliderRow(c, "Intensitas pantulan", 0, 200, 10, (s.animIntensity * 100).toInt(), "%") { v ->
            prefs.update { it.copy(animIntensity = v / 100f) }
        })
        val group = RadioGroup(c)
        val styles = listOf(
            IslandSettings.STYLE_SPRING to "Spring (natural)",
            IslandSettings.STYLE_SMOOTH to "Halus (tanpa pantulan)",
            IslandSettings.STYLE_ELASTIC to "Elastis (pantulan besar)"
        )
        styles.forEachIndexed { i, (style, label) ->
            group.addView(RadioButton(c).apply {
                id = View.generateViewId()
                text = label
                setTextColor(Ui.TEXT)
                isChecked = s.animStyle == style
                setOnClickListener { prefs.update { it.copy(animStyle = style) } }
            }, i)
        }
        anim.addView(group)
        root.addView(anim)

        root.addView(Ui.button(c, "Kembalikan ke bawaan") {
            prefs.update {
                IslandSettings().copy(
                    notificationsEnabled = it.notificationsEnabled, musicEnabled = it.musicEnabled,
                    chargingEnabled = it.chargingEnabled, volumeEnabled = it.volumeEnabled,
                    connectivityEnabled = it.connectivityEnabled, timerEnabled = it.timerEnabled,
                    hapticEnabled = it.hapticEnabled, notificationTimeoutSec = it.notificationTimeoutSec,
                    notificationApps = it.notificationApps, hiddenApps = it.hiddenApps
                )
            }
            activity.recreate()
        })
        return root
    }

    private fun swatches(colors: List<Long>, onPick: (Int) -> Unit): View {
        val c = activity
        return LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            colors.forEach { value ->
                val color = value.toInt()
                addView(View(c).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                        setStroke(c.dp(2), 0xFF5A5A60.toInt())
                    }
                    setOnClickListener { onPick(color) }
                }, LinearLayout.LayoutParams(c.dp(40), c.dp(40)).apply {
                    marginEnd = c.dp(12)
                    topMargin = c.dp(6)
                })
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
