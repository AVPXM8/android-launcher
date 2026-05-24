package com.vivex.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.vivex.launcher.cache.IconCache
import com.vivex.launcher.constants.LauncherConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository in charge of loading and sorting installed launcher applications.
 * Executes on [Dispatchers.IO] to keep the main thread unblocked and loads icons into [IconCache] proactively.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Queries the system [PackageManager] for launchable launcher activities, sorts them alphabetically,
     * and pre-caches their icons in memory.
     *
     * @return Alphabetically ordered list of launchable [AppModel] items.
     */
    suspend fun getInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Query PackageManager for all matching activities
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)
        val appList = ArrayList<AppModel>(resolveInfos.size)

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val className = resolveInfo.activityInfo.name
            
            // Skip the launcher itself or other unwanted system components
            if (LauncherConfig.IGNORED_PACKAGES.contains(packageName)) {
                continue
            }

            // Retrieve user-facing label
            val label = resolveInfo.loadLabel(packageManager).toString()
            val appModel = AppModel(
                label = label,
                packageName = packageName,
                className = className
            )
            appList.add(appModel)

            // Warm up the icon cache on this background thread if not present
            if (IconCache.get(packageName) == null) {
                try {
                    val icon = resolveInfo.loadIcon(packageManager)
                    IconCache.put(packageName, icon)
                } catch (e: Exception) {
                    val defaultIcon = packageManager.defaultActivityIcon
                    IconCache.put(packageName, defaultIcon)
                }
            }
        }

        // Sort alphabetically case-insensitively using standard Kotlin collator optimization
        appList.sortWith { app1, app2 ->
            app1.label.compareTo(app2.label, ignoreCase = true)
        }

        appList
    }
}
