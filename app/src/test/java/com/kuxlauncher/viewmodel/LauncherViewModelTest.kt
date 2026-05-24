package com.kuxlauncher.viewmodel

import com.kuxlauncher.model.WorkspaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Standard unit tests for launcher model behaviors and structure.
 */
class LauncherViewModelTest {

    @Test
    fun testWorkspaceItem_shortcutProperties() {
        val shortcut = WorkspaceItem(
            id = 1L,
            itemType = WorkspaceItem.ITEM_TYPE_SHORTCUT,
            label = "Phone",
            packageName = "com.android.phone",
            className = "com.android.phone.DialtactsActivity",
            cellX = 2,
            cellY = 3,
            container = WorkspaceItem.CONTAINER_DESKTOP
        )

        assertFalse(shortcut.isFolder)
        assertFalse(shortcut.isInFolder)
        assertEquals(1L, shortcut.id)
        assertEquals("Phone", shortcut.label)
        assertEquals("com.android.phone", shortcut.packageName)
        assertEquals("com.android.phone.DialtactsActivity", shortcut.className)
        assertEquals(2, shortcut.cellX)
        assertEquals(3, shortcut.cellY)
        assertEquals(WorkspaceItem.CONTAINER_DESKTOP, shortcut.container)
    }

    @Test
    fun testWorkspaceItem_folderProperties() {
        val folder = WorkspaceItem(
            id = 2L,
            itemType = WorkspaceItem.ITEM_TYPE_FOLDER,
            label = "Work Apps",
            packageName = null,
            className = null,
            cellX = 0,
            cellY = 1,
            container = WorkspaceItem.CONTAINER_DOCK
        )

        val itemInFolder = WorkspaceItem(
            id = 3L,
            itemType = WorkspaceItem.ITEM_TYPE_SHORTCUT,
            label = "Mail",
            packageName = "com.google.android.gm",
            className = "com.google.android.gm.ConversationListActivityGmail",
            cellX = 0,
            cellY = 0,
            container = 2 // Inside folder 2
        )
        folder.folderItems.add(itemInFolder)

        assertTrue(folder.isFolder)
        assertFalse(folder.isInFolder)
        
        assertTrue(itemInFolder.isInFolder)
        assertFalse(itemInFolder.isFolder)
        assertEquals(2, itemInFolder.container)
        assertEquals(1, folder.folderItems.size)
        assertEquals("Mail", folder.folderItems[0].label)
    }
}
