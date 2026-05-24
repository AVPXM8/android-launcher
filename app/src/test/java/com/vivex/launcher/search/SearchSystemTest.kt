package com.vivex.launcher.search

import com.vivex.launcher.cache.AppInfoCache
import com.vivex.launcher.data.AppModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying indexing, asynchronous matching, cache operations, and highlighting logic.
 */
class SearchSystemTest {

    @Test
    fun testAppInfoCache_getSetClear() {
        val apps = listOf(
            AppModel("Camera", "com.android.camera", "com.android.camera.Camera"),
            AppModel("Phone", "com.android.phone", "com.android.phone.DialtactsActivity")
        )

        assertNull(AppInfoCache.get())
        AppInfoCache.set(apps)
        assertEquals(apps, AppInfoCache.get())

        AppInfoCache.clear()
        assertNull(AppInfoCache.get())
    }

    @Test
    fun testSearchIndexer_matching() {
        val apps = listOf(
            AppModel("Camera", "com.android.camera", "com.android.camera.Camera"),
            AppModel("Chrome", "com.android.chrome", "com.google.android.apps.chrome.Main"),
            AppModel("Settings", "com.android.settings", "com.android.settings.Settings")
        )

        SearchIndexer.buildIndex(apps)

        // 1. Match by prefix / substring (case-insensitive)
        val cameraResult = SearchIndexer.search("cam")
        assertEquals(1, cameraResult.size)
        assertEquals("Camera", cameraResult[0].label)

        // 2. Match by package name
        val chromeResult = SearchIndexer.search("chrome")
        assertEquals(1, chromeResult.size)
        assertEquals("Chrome", chromeResult[0].label)

        // 3. Match distinct queries
        val settingsResult = SearchIndexer.search("sett")
        assertEquals(1, settingsResult.size)
        assertEquals("Settings", settingsResult[0].label)

        // 4. Empty query matches all
        val emptyResult = SearchIndexer.search("")
        assertEquals(3, emptyResult.size)
    }

    @Test
    fun testSearchHighlighter_emptyQuery() {
        val text = "Google Chrome"
        val query = ""
        val highlightColor = -16711936 // 0xFF00FF00 as signed Int

        // Highlighting with a blank query should bypass SpannableString creation
        // and return the target text directly, preventing Stub exceptions on a local JVM.
        val highlighted = SearchHighlighter.highlight(text, query, highlightColor)
        assertEquals(text, highlighted.toString())
    }
}
