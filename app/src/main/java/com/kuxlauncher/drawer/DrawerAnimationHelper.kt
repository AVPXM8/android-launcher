package com.kuxlauncher.drawer

import android.view.View
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.kuxlauncher.animation.SpringAnimationHelper
import com.kuxlauncher.animation.DrawerTransitionHelper

/**
 * Orchestrates open and close spring animations for the full-screen App Drawer.
 */
class DrawerAnimationHelper(
    private val drawerView: View,
    private val workspaceView: View,
    private val stateManager: DrawerStateManager
) {

    private var activeSpring: SpringAnimation? = null

    /**
     * Slides the drawer to fully opened (translationY = 0) using spring physics.
     */
    fun animateOpen(onComplete: (() -> Unit)? = null) {
        cancelActiveAnimation()
        stateManager.setState(DrawerStateManager.DrawerState.ANIMATING)
        
        val totalHeight = drawerView.height.toFloat()
        activeSpring = SpringAnimationHelper.createTranslationYSpring(
            view = drawerView,
            finalPosition = 0f,
            stiffness = SpringForce.STIFFNESS_MEDIUM,
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY,
            onAnimationEnd = {
                stateManager.setState(DrawerStateManager.DrawerState.OPENED)
                onComplete?.invoke()
                activeSpring = null
            }
        ).apply {
            // Apply visual progress updates while animating
            addUpdateListener { _, value, _ ->
                DrawerTransitionHelper.updateProgress(drawerView, workspaceView, value, totalHeight)
            }
            start()
        }
    }

    /**
     * Slides the drawer to fully closed (translationY = height) using spring physics.
     */
    fun animateClose(onComplete: (() -> Unit)? = null) {
        cancelActiveAnimation()
        stateManager.setState(DrawerStateManager.DrawerState.ANIMATING)
        
        val closedY = drawerView.height.toFloat()
        activeSpring = SpringAnimationHelper.createTranslationYSpring(
            view = drawerView,
            finalPosition = closedY,
            stiffness = SpringForce.STIFFNESS_MEDIUM,
            dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY,
            onAnimationEnd = {
                stateManager.setState(DrawerStateManager.DrawerState.CLOSED)
                onComplete?.invoke()
                activeSpring = null
            }
        ).apply {
            addUpdateListener { _, value, _ ->
                DrawerTransitionHelper.updateProgress(drawerView, workspaceView, value, closedY)
            }
            start()
        }
    }

    /**
     * Instantly sets the drawer position without animation (e.g. during manual drag tracking).
     */
    fun setTranslationYDirect(translationY: Float) {
        cancelActiveAnimation()
        val totalHeight = drawerView.height.toFloat()
        DrawerTransitionHelper.updateProgress(
            drawerView = drawerView,
            workspaceView = workspaceView,
            translationY = translationY.coerceIn(0f, totalHeight),
            maxTranslationY = totalHeight
        )
    }

    /**
     * Cancels any ongoing spring animations.
     */
    fun cancelActiveAnimation() {
        activeSpring?.cancel()
        activeSpring = null
    }
}
