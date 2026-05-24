package com.kuxlauncher.animation

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

/**
 * High-performance animation helper leveraging Android's DynamicAnimation Spring physics
 * to create butter-smooth, Pixel/Nova style elastic transitions for launcher panels.
 */
object SpringAnimationHelper {

    /**
     * Creates and configures a SpringAnimation for a View's translationY property.
     * @param view View target to animate
     * @param finalPosition Target pixel coordinate to land on
     * @param stiffness Damping stiffness (e.g. SpringForce.STIFFNESS_MEDIUM)
     * @param dampingRatio Elasticity ratio (e.g. SpringForce.DAMPING_RATIO_NO_BOUNCY)
     */
    fun createTranslationYSpring(
        view: View,
        finalPosition: Float,
        stiffness: Float = SpringForce.STIFFNESS_MEDIUM,
        dampingRatio: Float = SpringForce.DAMPING_RATIO_NO_BOUNCY,
        onAnimationEnd: (() -> Unit)? = null
    ): SpringAnimation {
        val animation = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y)
        val springForce = SpringForce(finalPosition).apply {
            this.stiffness = stiffness
            this.dampingRatio = dampingRatio
        }
        animation.spring = springForce
        if (onAnimationEnd != null) {
            animation.addEndListener { _, _, _, _ ->
                onAnimationEnd()
            }
        }
        return animation
    }
}
