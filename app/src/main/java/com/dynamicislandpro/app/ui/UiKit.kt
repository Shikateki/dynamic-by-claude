package com.dynamicislandpro.app.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

/** Pembuat view kecil agar halaman tidak bergantung pada banyak file layout XML. */
object Ui {
    const val TEXT = 0xFFFFFFFF.toInt()
    const val MUTED = 0xFFA0A0A8.toInt()
    const val CARD = 0xFF1C1C1E.toInt()
    const val GOOD = 0xFF30D158.toInt()
    const val BAD = 0xFFFF453A.toInt()
    const val WARN = 0xFFFFD60A.toInt()

    fun page(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(24))
    }

    fun title(ctx: Context, text: String) = TextView(ctx).apply {
        this.text = text
        setTextColor(TEXT)
        textSize = 24f
        setPadding(0, ctx.dp(8), 0, ctx.dp(12))
    }

    fun heading(ctx: Context, text: String) = TextView(ctx).apply {
        this.text = text
        setTextColor(TEXT)
        textSize = 16f
        setPadding(0, 0, 0, ctx.dp(8))
    }

    fun body(ctx: Context, text: String) = TextView(ctx).apply {
        this.text = text
        setTextColor(MUTED)
        textSize = 13f
        setLineSpacing(0f, 1.15f)
    }

    fun card(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(16), ctx.dp(14), ctx.dp(16), ctx.dp(14))
        background = GradientDrawable().apply {
            cornerRadius = ctx.dp(16).toFloat()
            setColor(CARD)
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = ctx.dp(12) }
    }

    fun button(ctx: Context, text: String, onClick: () -> Unit) = MaterialButton(ctx).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dp(6) }
    }

    fun row(ctx: Context) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun switchRow(ctx: Context, label: String, checked: Boolean, onChange: (Boolean) -> Unit): View =
        row(ctx).apply {
            setPadding(0, ctx.dp(4), 0, ctx.dp(4))
            addView(TextView(ctx).apply {
                text = label
                setTextColor(TEXT)
                textSize = 15f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(MaterialSwitch(ctx).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, v -> onChange(v) }
            })
        }

    /** Slider bilangan bulat. (to - from) harus kelipatan step. */
    fun sliderRow(
        ctx: Context, label: String, from: Int, to: Int, step: Int, value: Int,
        suffix: String, onChange: (Int) -> Unit
    ): View {
        val snapped = (from + ((value - from).toFloat() / step).roundToInt() * step).coerceIn(from, to)
        val caption = TextView(ctx).apply {
            text = "$label: $snapped$suffix"
            setTextColor(TEXT)
            textSize = 14f
        }
        val slider = Slider(ctx).apply {
            valueFrom = from.toFloat()
            valueTo = to.toFloat()
            stepSize = step.toFloat()
            this.value = snapped.toFloat()
            addOnChangeListener { _, v, fromUser ->
                if (fromUser) {
                    val i = v.roundToInt()
                    caption.text = "$label: $i$suffix"
                    onChange(i)
                }
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, ctx.dp(4), 0, 0)
            addView(caption)
            addView(slider)
        }
    }

    /** Baris status izin/mode: titik berwarna + teks + tombol aksi. */
    class StatusRow(ctx: Context, name: String, hint: String, actionLabel: String?, onAction: (() -> Unit)?) {
        private val dot = View(ctx)
        private val state = TextView(ctx)
        val view: View

        init {
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(ctx).apply { text = name; setTextColor(Ui.TEXT); textSize = 15f })
            col.addView(Ui.body(ctx, hint))
            state.textSize = 12f
            col.addView(state)

            val line = Ui.row(ctx).apply { setPadding(0, ctx.dp(8), 0, ctx.dp(8)) }
            line.addView(dot, LinearLayout.LayoutParams(ctx.dp(10), ctx.dp(10)).apply { marginEnd = ctx.dp(12) })
            line.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (actionLabel != null && onAction != null) {
                line.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = actionLabel
                    isAllCaps = false
                    setOnClickListener { onAction() }
                })
            }
            view = line
        }

        fun set(ok: Boolean, text: String, neutral: Boolean = false) {
            val color = if (ok) Ui.GOOD else if (neutral) Ui.WARN else Ui.BAD
            dot.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            state.text = text
            state.setTextColor(color)
        }
    }
}
