package com.kuxlauncher.data

/**
 * Highly lightweight data model representing an installed Android application.
 * Devoid of heavy raw Drawables (which are cached separately in LruCache) to prevent GC pressure and memory leaks.
 */
data class AppModel(
    val label: String,
    val packageName: String,
    val className: String
) {
    /**
     * Unique identifier derived from the package and class name.
     * Used for RecyclerView Stable IDs.
     */
    val stableId: Long = (packageName.hashCode().toLong() shl 32) or (className.hashCode().toLong() and 0xFFFFFFFFL)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppModel

        if (label != other.label) return false
        if (packageName != other.packageName) return false
        if (className != other.className) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }
}
