package com.kuxlauncher.constants

/**
 * Unified configuration parameters for Kux Launcher.
 */
object LauncherConfig {
    // Log tag for the application
    const val TAG = "KuxLauncher"

    // Default column counts as fallback
    const val DEFAULT_COLUMN_COUNT = 4

    // Dynamic grid layout item size in DP (app icons with labels)
    // Used to calculate dynamic columns based on actual screen width
    const val IDEAL_GRID_ITEM_WIDTH_DP = 85

    // Fraction of maximum runtime memory to allocate to the Icon Cache
    // 0.125 represents 1/8th of the total available VM memory
    const val ICON_CACHE_MEMORY_FRACTION = 0.125f

    // Package names to ignore in launcher (e.g. the launcher itself)
    val IGNORED_PACKAGES = setOf(
        "com.kuxlauncher",
        "com.android.keyguard"
    )
}
