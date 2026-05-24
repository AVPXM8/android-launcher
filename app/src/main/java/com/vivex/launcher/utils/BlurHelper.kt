package com.vivex.launcher.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Utility to manage and apply premium visual glassmorphism style assets
 * to launcher drawer and folder components.
 */
object BlurHelper {

    /**
     * Applies a premium dark glassmorphic background to the target view.
     * Combines deep semi-transparent colors and smooth corner radiuses.
     */
    fun applyGlassmorphism(view: View, cornerRadiusPx: Float = 0f) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            // Beautiful, extremely premium dark grey translucent color (90% opacity)
            setColor(Color.parseColor("#E6161616"))
            if (cornerRadiusPx > 0) {
                // Top-left, top-right, bottom-right, bottom-left radiuses (8 values)
                cornerRadii = floatArrayOf(
                    cornerRadiusPx, cornerRadiusPx, // Top-left
                    cornerRadiusPx, cornerRadiusPx, // Top-right
                    0f, 0f,                         // Bottom-right
                    0f, 0f                          // Bottom-left
                )
            }
        }
        view.background = drawable
    }
}
