package com.kuxlauncher.animation

import android.view.View

/**
 * Handles drawer transition side-effects like fading, scaling, and darkening overlays
 * based on the current translation distance of the drawer view.
 */
object DrawerTransitionHelper {

    /**
     * Updates background alpha, drawer alpha, and workspace scale based on the translation fraction.
     * @param drawerView The full-screen drawer layout
     * @param workspaceView The workspace container layout that gets scaled slightly
     * @param translationY Current translation Y of the drawer view
     * @param maxTranslationY The screen height (maximum translation distance)
     */
    fun updateProgress(
        drawerView: View,
        workspaceView: View,
        translationY: Float,
        maxTranslationY: Float
    ) {
        if (maxTranslationY <= 0f) {
            return
        }
        
        // Progress ranges from 0.0 (fully open) to 1.0 (fully closed)
        val fraction = (translationY / maxTranslationY).coerceIn(0f, 1f)
        val openProgress = 1f - fraction // 1.0 (fully open) to 0.0 (fully closed)

        // 1. App Drawer visual properties
        drawerView.translationY = translationY
        // Smoothly fade drawer out as it approaches closed state
        drawerView.alpha = if (openProgress < 0.1f) openProgress / 0.1f else 1f

        // 2. Premium visual feedback: workspace scales down slightly (depth effect)
        // 1.0 (closed) -> 0.95 (open)
        val scale = 1f - (0.05f * openProgress)
        workspaceView.scaleX = scale
        workspaceView.scaleY = scale
        // Fade the workspace slightly as the drawer covers it
        workspaceView.alpha = 1f - (0.5f * openProgress)
    }
}
