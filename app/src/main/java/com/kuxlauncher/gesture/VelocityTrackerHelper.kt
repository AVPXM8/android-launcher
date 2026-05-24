package com.kuxlauncher.gesture

import android.view.MotionEvent
import android.view.VelocityTracker

/**
 * Encapsulates the android.view.VelocityTracker utility to ensure clean gesture speed tracking
 * during swipe actions.
 */
class VelocityTrackerHelper {
    private var velocityTracker: VelocityTracker? = null

    /**
     * Obtains or resets the VelocityTracker instance.
     */
    fun start() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker?.clear()
        }
    }

    /**
     * Adds standard motion events to the velocity tracker.
     */
    fun addMovement(event: MotionEvent) {
        velocityTracker?.addMovement(event)
    }

    /**
     * Computes the current y-velocity of the touch gesture in pixels per second.
     */
    fun computeVelocity(units: Int = 1000): Float {
        velocityTracker?.computeCurrentVelocity(units)
        return velocityTracker?.yVelocity ?: 0f
    }

    /**
     * Recycles the velocity tracker instance to prevent memory leaks.
     */
    fun recycle() {
        velocityTracker?.recycle()
        velocityTracker = null
    }
}
