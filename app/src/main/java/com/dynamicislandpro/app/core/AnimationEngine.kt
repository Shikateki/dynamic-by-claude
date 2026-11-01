package com.dynamicislandpro.app.core

import android.view.View
import android.widget.FrameLayout
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * Semua animasi Island: morphing ukuran (spring), muncul/hilang,
 * transisi konten (slide + fade) dan pegas balik untuk gesture.
 */
class AnimationEngine(private val settings: () -> IslandSettings) {

    private var sizeAnim: SpringAnimation? = null

    private fun dampingRatio(): Float {
        val s = settings()
        return when (s.animStyle) {
            IslandSettings.STYLE_SMOOTH -> 1f
            IslandSettings.STYLE_ELASTIC -> (0.5f - 0.15f * s.animIntensity).coerceIn(0.2f, 0.6f)
            else -> (0.9f - 0.3f * s.animIntensity).coerceIn(0.3f, 0.95f)
        }
    }

    private fun stiffness(): Float = (700f * settings().animSpeed).coerceIn(150f, 3000f)

    private fun newSpring(target: Float): SpringForce =
        SpringForce(target).setDampingRatio(dampingRatio()).setStiffness(stiffness())

    /** Durasi dasar untuk animasi non-spring (fade/slide), mengikuti kecepatan. */
    fun durationMs(base: Long = 220L): Long = (base / settings().animSpeed).toLong().coerceIn(60L, 800L)

    /**
     * Morphing ukuran + radius sudut. [onFrame] dipanggil setiap frame dengan
     * progres 0..1 (boleh sedikit >1 karena overshoot pegas).
     */
    fun morph(onFrame: (Float) -> Unit, onEnd: (() -> Unit)? = null) {
        sizeAnim?.cancel()
        val holder = FloatValueHolder(0f)
        val anim = SpringAnimation(holder).setSpring(newSpring(1f))
        anim.setMinimumVisibleChange(0.002f)
        anim.addUpdateListener { _, value, _ -> onFrame(value) }
        anim.addEndListener { _, canceled, _, _ ->
            if (!canceled) {
                onFrame(1f)
                onEnd?.invoke()
            }
        }
        sizeAnim = anim
        anim.start()
    }

    /** Mengubah ukuran FrameLayout.LayoutParams view secara halus. */
    fun resize(view: View, toW: Int, toH: Int, minW: Int, minH: Int, onEnd: (() -> Unit)? = null,
               onProgress: ((Float) -> Unit)? = null) {
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val fromW = if (lp.width > 0) lp.width else view.width
        val fromH = if (lp.height > 0) lp.height else view.height
        morph(onFrame = { p ->
            lp.width = (fromW + (toW - fromW) * p).toInt().coerceAtLeast(minW)
            lp.height = (fromH + (toH - fromH) * p).toInt().coerceAtLeast(minH)
            view.layoutParams = lp
            onProgress?.invoke(p)
        }, onEnd = onEnd)
    }

    fun appear(view: View) {
        view.animate().cancel()
        view.alpha = 0f
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.animate().alpha(1f).setDuration(durationMs(160)).start()
        for (prop in arrayOf(DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y)) {
            val anim = SpringAnimation(view, prop, 1f)
            anim.setSpring(newSpring(1f))
            anim.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE)
            anim.start()
        }
    }

    fun disappear(view: View, onEnd: () -> Unit) {
        view.animate().cancel()
        view.animate()
            .alpha(0f).scaleX(0.6f).scaleY(0.6f)
            .setDuration(durationMs(180))
            .withEndAction(onEnd)
            .start()
    }

    /** Ganti isi: konten lama slide+fade keluar, konten baru slide+fade masuk. */
    fun swapContent(content: View, direction: Int, update: () -> Unit) {
        content.animate().cancel()
        val shift = 28f * content.resources.displayMetrics.density * (if (direction == 0) 0.4f else 1f)
        val sign = if (direction >= 0) 1f else -1f
        val half = durationMs(110)
        content.animate().alpha(0f).translationX(-sign * shift).setDuration(half).withEndAction {
            update()
            content.translationX = sign * shift
            content.animate().alpha(1f).translationX(0f).setDuration(half).start()
        }.start()
    }

    /** Pegas balik ke posisi awal setelah drag (gesture-based animation). */
    fun springBack(view: View) {
        val anim = SpringAnimation(view, DynamicAnimation.TRANSLATION_X, 0f)
        anim.setSpring(newSpring(0f))
        anim.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS)
        anim.start()
    }

    fun cancelAll() {
        sizeAnim?.cancel()
        sizeAnim = null
    }
}
