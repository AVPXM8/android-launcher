package com.kuxlauncher.model

/**
 * Modern data model representing an item placed on the custom home screen.
 * Can represent either a standard App Shortcut or a Folder containing sub-shortcuts.
 */
data class WorkspaceItem(
    val id: Long = 0,
    val itemType: Int, // ITEM_TYPE_SHORTCUT or ITEM_TYPE_FOLDER
    var label: String,
    val packageName: String?,
    val className: String?,
    var cellX: Int,
    var cellY: Int,
    var container: Int, // CONTAINER_DESKTOP, CONTAINER_DOCK, or a folderId
    val folderItems: MutableList<WorkspaceItem> = mutableListOf()
) {
    companion object {
        const val ITEM_TYPE_SHORTCUT = 1
        const val ITEM_TYPE_FOLDER = 2

        const val CONTAINER_DESKTOP = -100
        const val CONTAINER_DOCK = -101
    }

    /**
     * Helper to check if this item is a folder.
     */
    val isFolder: Boolean
        get() = itemType == ITEM_TYPE_FOLDER

    /**
     * Helper to check if this item is inside a folder.
     */
    val isInFolder: Boolean
        get() = container > 0
}
