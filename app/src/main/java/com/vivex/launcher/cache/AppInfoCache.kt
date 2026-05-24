package com.vivex.launcher.cache

import com.vivex.launcher.data.AppModel
import java.util.Collections

/**
 * Thread-safe local cache for preserving the list of installed application launch targets.
 * Avoids repeated and slow PackageManager calls, keeping launcher cold startup times extremely low.
 */
object AppInfoCache {
    @Volatile
    private var cachedApps: List<AppModel>? = null

    /**
     * Updates the cached app list.
     */
    fun set(apps: List<AppModel>) {
        synchronized(this) {
            cachedApps = Collections.unmodifiableList(ArrayList(apps))
        }
    }

    /**
     * Retrieves the cached app list, or null if it has not been set yet.
     */
    fun get(): List<AppModel>? {
        synchronized(this) {
            return cachedApps
        }
    }

    /**
     * Evicts the cached list, forcing a package reload.
     */
    fun clear() {
        synchronized(this) {
            cachedApps = null
        }
    }
}
