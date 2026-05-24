package com.kuxlauncher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kuxlauncher.data.AppModel
import com.kuxlauncher.data.AppRepository
import com.kuxlauncher.database.DatabaseHelper
import com.kuxlauncher.model.WorkspaceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standard Jetpack ViewModel orchestrating launcher state, installed applications list,
 * and persistent workspace items (shortcuts & folders) using Coroutines on background threads.
 */
class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val dbHelper = DatabaseHelper(application)

    private val _appList = MutableLiveData<List<AppModel>>()
    val appList: LiveData<List<AppModel>> get() = _appList

    private val _workspaceItems = MutableLiveData<List<WorkspaceItem>>()
    val workspaceItems: LiveData<List<WorkspaceItem>> get() = _workspaceItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    /**
     * Retrieves the list of installed application launch targets alphabetically from PackageManager.
     */
    fun loadApps(forceReload: Boolean = false) {
        if (_appList.value != null && !forceReload) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val apps = appRepository.getInstalledApps()
                _appList.postValue(apps)
            } catch (e: Exception) {
                _appList.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Queries and refreshes all workspace items (shortcuts and folders) from the persistent SQLite database.
     */
    fun loadWorkspaceItems() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = dbHelper.getAllWorkspaceItems()
                _workspaceItems.postValue(items)
            } catch (e: Exception) {
                _workspaceItems.postValue(emptyList())
            }
        }
    }

    /**
     * Inserts an App Shortcut onto the workspace grid at a specific cell position.
     */
    fun addShortcutToWorkspace(app: AppModel, cellX: Int, cellY: Int, container: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = WorkspaceItem(
                itemType = WorkspaceItem.ITEM_TYPE_SHORTCUT,
                label = app.label,
                packageName = app.packageName,
                className = app.className,
                cellX = cellX,
                cellY = cellY,
                container = container
            )
            dbHelper.insertItem(item)
            loadWorkspaceItems()
        }
    }

    /**
     * Updates coordinates and container layout of an existing desktop item (shortcut or folder).
     */
    fun updateItemPosition(item: WorkspaceItem, cellX: Int, cellY: Int, container: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            item.cellX = cellX
            item.cellY = cellY
            item.container = container
            dbHelper.updateItemPosition(item.id, cellX, cellY, container)
            loadWorkspaceItems()
        }
    }

    /**
     * Combines two existing workspace shortcuts into a brand-new folder, persisting inside SQLite transaction.
     */
    fun mergeShortcutsToFolder(
        shortcut1: WorkspaceItem,
        shortcut2: WorkspaceItem,
        cellX: Int,
        cellY: Int,
        container: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val folderLabel = "Folder"
            dbHelper.createFolderWithItems(folderLabel, shortcut1, shortcut2, cellX, cellY, container)
            loadWorkspaceItems()
        }
    }

    /**
     * Creates a folder by merging a newly dragged drawer app onto an existing workspace shortcut.
     */
    fun mergeDrawerAppWithShortcut(
        app: AppModel,
        targetShortcut: WorkspaceItem,
        cellX: Int,
        cellY: Int,
        container: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val appShortcut = WorkspaceItem(
                itemType = WorkspaceItem.ITEM_TYPE_SHORTCUT,
                label = app.label,
                packageName = app.packageName,
                className = app.className,
                cellX = 0,
                cellY = 0,
                container = container
            )
            val newAppId = dbHelper.insertItem(appShortcut)
            val insertedAppShortcut = appShortcut.copy(id = newAppId)
            dbHelper.createFolderWithItems("Folder", targetShortcut, insertedAppShortcut, cellX, cellY, container)
            loadWorkspaceItems()
        }
    }

    /**
     * Adds an app shortcut inside an existing desktop folder.
     */
    fun addShortcutToFolder(shortcut: WorkspaceItem, folder: WorkspaceItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update container in DB to match folder ID. Clear desktop coordinates.
            dbHelper.updateItemPosition(shortcut.id, 0, 0, folder.id.toInt())
            loadWorkspaceItems()
        }
    }

    /**
     * Adds a newly dragged drawer app shortcut directly inside an existing desktop folder.
     */
    fun addDrawerAppToFolder(app: AppModel, folder: WorkspaceItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = WorkspaceItem(
                itemType = WorkspaceItem.ITEM_TYPE_SHORTCUT,
                label = app.label,
                packageName = app.packageName,
                className = app.className,
                cellX = 0,
                cellY = 0,
                container = folder.id.toInt()
            )
            dbHelper.insertItem(item)
            loadWorkspaceItems()
        }
    }

    /**
     * Programmatically removes an item (shortcut, folder, or drawer app drag out) from the home screen workspace.
     */
    fun removeItem(item: WorkspaceItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.deleteItem(item.id)
            loadWorkspaceItems()
        }
    }

    /**
     * Renames a folder's label in database.
     */
    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.updateFolderLabel(folderId, newName)
            loadWorkspaceItems()
        }
    }
}
