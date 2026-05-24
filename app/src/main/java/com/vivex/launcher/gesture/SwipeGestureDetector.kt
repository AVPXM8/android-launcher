package com.vivex.launcher.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Custom touch listener to capture upward swipe gestures on the home screen
 * to open the premium app drawer, and downward swipes inside the drawer to dismiss it.
 */
class SwipeGestureDetector(
    context: Context,
    private val onSwipeUp: () -> Unit,
    private val onSwipeDown: () -> Unit
) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        // Always return true or process standard events
        val handled = gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        return handled
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 80
        private val swipeVelocityThreshold = 80

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) {
                return false
            }
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            
            // Check if vertical gesture predominates
            if (abs(diffY) > abs(diffX)) {
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY < 0) {
                        // Swipe up
                        onSwipeUp()
                        return true
                    } else {
                        // Swipe down
                        onSwipeDown()
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }
}
