package com.vibe.ui.theme

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Vibe animation utilities.
 */
object VibeAnimations {

    /**
     * Fade in animation.
     */
    fun fadeIn(view: View, duration: Long = 300, delay: Long = 0) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Fade out animation.
     */
    fun fadeOut(view: View, duration: Long = 300, delay: Long = 0, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    /**
     * Slide up animation.
     */
    fun slideUp(view: View, duration: Long = 400, delay: Long = 0) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    /**
     * Slide down animation.
     */
    fun slideDown(view: View, duration: Long = 400, delay: Long = 0, onEnd: (() -> Unit)? = null) {
        view.animate()
            .translationY(view.height.toFloat())
            .alpha(0f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    /**
     * Scale up animation (pop in).
     */
    fun scaleIn(view: View, duration: Long = 300, delay: Long = 0) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    /**
     * Pulse animation.
     */
    fun pulse(view: View, duration: Long = 1000, repeat: Int = ObjectAnimator.INFINITE) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        scaleX.repeatCount = repeat
        scaleY.repeatCount = repeat
        scaleX.duration = duration
        scaleY.duration = duration

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /**
     * Shake animation (for errors).
     */
    fun shake(view: View, duration: Long = 500) {
        val shake = ObjectAnimator.ofFloat(view, "translationX",
            0f, 25f, -25f, 20f, -20f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        shake.duration = duration
        shake.start()
    }

    /**
     * Bounce animation.
     */
    fun bounce(view: View, duration: Long = 600) {
        view.animate()
            .scaleY(0.8f)
            .setDuration(duration / 3)
            .withEndAction {
                view.animate()
                    .scaleY(1.1f)
                    .setDuration(duration / 3)
                    .withEndAction {
                        view.animate()
                            .scaleY(1f)
                            .setDuration(duration / 3)
                            .start()
                    }
                    .start()
            }
            .start()
    }
}
