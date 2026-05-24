package com.kuxlauncher.cache

import android.graphics.drawable.Drawable
import android.util.LruCache
import com.kuxlauncher.constants.LauncherConfig

/**
 * Thread-safe LruCache for in-memory caching of Application Icons to ensure butter-smooth scrolling.
 *decodes and stores raw app drawables asynchronously, size-constrained by total runtime memory.
 */
object IconCache {
    
    // Calculate total cache size limit (in bytes) based on configuration fraction of JVM maximum memory
    private val maxMemoryBytes = Runtime.getRuntime().maxMemory()
    private val cacheSize = (maxMemoryBytes * LauncherConfig.ICON_CACHE_MEMORY_FRACTION).toInt()

    // Inner LruCache checking size using pixel byte overhead estimates
    private val cache = object : LruCache<String, Drawable>(cacheSize) {
        override fun sizeOf(key: String, drawable: Drawable): Int {
            val width = drawable.intrinsicWidth.coerceAtLeast(48)
            val height = drawable.intrinsicHeight.coerceAtLeast(48)
            // Treat as standard 32-bit ARGB (4 bytes per pixel)
            return width * height * 4
        }
    }

    /**
     * Retrieve an icon from the cache.
     * @param packageName Unique application package identifier
     * @return Cached Drawable, or null if absent
     */
    fun get(packageName: String): Drawable? {
        synchronized(cache) {
            return cache.get(packageName)
        }
    }

    /**
     * Store an icon in the cache.
     * @param packageName Unique application package identifier
     * @param icon Icon Drawable to cache
     */
    fun put(packageName: String, icon: Drawable) {
        synchronized(cache) {
            if (cache.get(packageName) == null) {
                cache.put(packageName, icon)
            }
        }
    }

    /**
     * Evicts all cached icons. Call during low-memory signals.
     */
    fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
    }
}
