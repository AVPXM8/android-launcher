package com.kuxlauncher.search

import com.kuxlauncher.data.AppModel
import java.util.Locale

/**
 * Background search engine indexing component.
 * Precomputes lowercase names and package representations to allow instant,
 * non-blocking queries on a worker coroutine context.
 */
object SearchIndexer {

    class IndexedApp(
        val app: AppModel,
        val normalizedLabel: String,
        val normalizedPackage: String
    )

    @Volatile
    private var index: List<IndexedApp> = emptyList()

    /**
     * Rebuilds the search index with a new list of applications.
     * Call this in a background thread when application list changes.
     */
    fun buildIndex(apps: List<AppModel>) {
        val locale = Locale.getDefault()
        index = apps.map { app ->
            IndexedApp(
                app = app,
                normalizedLabel = app.label.lowercase(locale),
                normalizedPackage = app.packageName.lowercase(locale)
            )
        }
    }

    /**
     * Filters the index based on the search query.
     * Matches against either the app label or package name.
     */
    fun search(query: String): List<AppModel> {
        if (query.isBlank()) {
            return index.map { it.app }
        }

        val locale = Locale.getDefault()
        val normalizedQuery = query.trim().lowercase(locale)

        return index.filter { indexed ->
            indexed.normalizedLabel.contains(normalizedQuery) ||
                    indexed.normalizedPackage.contains(normalizedQuery)
        }.map { it.app }
    }
}
