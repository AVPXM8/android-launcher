package com.vivex.launcher.drawer

import android.view.MotionEvent
import android.view.View
import com.vivex.launcher.gesture.VelocityTrackerHelper
import kotlin.math.max

/**
 * Handles real-time swipe gestures on the app drawer layout, coordinating swipe up/down
 * with transitions and state updates based on fingers Y coordinates and drag velocities.
 */
class DrawerGestureController(
    private val drawerView: View,
    private val animationHelper: DrawerAnimationHelper,
    private val stateManager: DrawerStateManager
) : View.OnTouchListener {

    private var startY = 0f
    private var initialTranslationY = 0f
    private val velocityTracker = VelocityTrackerHelper()
    private var isDragging = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        velocityTracker.addMovement(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.rawY
                initialTranslationY = drawerView.translationY
                if (!stateManager.isAnimating) {
                    velocityTracker.start()
                    isDragging = true
                    stateManager.setState(DrawerStateManager.DrawerState.DRAGGING)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    return false
                }
                val deltaY = event.rawY - startY
                val targetTranslationY = max(0f, initialTranslationY + deltaY)
                animationHelper.setTranslationYDirect(targetTranslationY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!isDragging) {
                    return false
                }
                isDragging = false

                val velocityY = velocityTracker.computeVelocity()
                velocityTracker.recycle()

                val currentTranslationY = drawerView.translationY
                val drawerHeight = drawerView.height.toFloat()

                // High speed swipe down closes it
                if (velocityY > 800f) {
                    animationHelper.animateClose()
                }
                // High speed swipe up opens it
                else if (velocityY < -800f) {
                    animationHelper.animateOpen()
                }
                // Positional check
                else {
                    if (currentTranslationY > drawerHeight / 2) {
                        animationHelper.animateClose()
                    } else {
                        animationHelper.animateOpen()
                    }
                }
                return true
            }
        }
        return false
    }
}
