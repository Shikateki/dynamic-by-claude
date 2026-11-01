package com.dynamicislandpro.app.core

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

/** Ikon berupa emoji/glyph di dalam lingkaran halus. Tanpa file gambar tambahan. */
class GlyphDrawable(private val glyph: String, private val sizePx: Int) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sizePx * 0.78f
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val y = b.exactCenterY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(glyph, b.exactCenterX(), y, paint)
    }

    override fun getIntrinsicWidth() = sizePx
    override fun getIntrinsicHeight() = sizePx
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Garis progres tipis dengan ujung membulat. progress < 0 berarti tersembunyi. */
class ProgressLineView(context: Context) : View(context) {
    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var value = 0f

    fun setColor(color: Int) {
        fill.color = color
        track.color = (color and 0x00FFFFFF) or 0x33000000
        invalidate()
    }

    fun setProgress(p: Float) {
        val v = p.coerceIn(0f, 1f)
        if (v != value) {
            value = v
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val r = height / 2f
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, r, r, track)
        rect.set(0f, 0f, width * value, height.toFloat())
        canvas.drawRoundRect(rect, r, r, fill)
    }
}

/** Equalizer 4 batang. Animasi hanya berjalan saat playing & terpasang di layar. */
class EqualizerView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var phase = 0f
    private var playing = false
    private var animator: ValueAnimator? = null

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        updateAnimator()
        invalidate()
    }

    private fun updateAnimator() {
        if (playing && isAttachedToWindow && animator == null) {
            animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
                duration = 900L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else if ((!playing || !isAttachedToWindow) && animator != null) {
            animator?.cancel()
            animator = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimator()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val bars = 4
        val gap = width * 0.12f
        val barW = (width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val level = if (playing) 0.25f + 0.75f * abs(sin(phase + i * 1.3f)) else 0.2f
            val h = height * level
            val left = i * (barW + gap)
            rect.set(left, height - h, left + barW, height.toFloat())
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }
}
