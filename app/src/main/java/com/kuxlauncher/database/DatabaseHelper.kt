package com.kuxlauncher.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.kuxlauncher.model.WorkspaceItem

/**
 * Custom SQLiteOpenHelper implementing interview-quality, performant, transaction-safe 
 * layout persistence for launcher items (shortcuts, folders) on home screen cells.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "KuxDatabaseHelper"
        private const val DATABASE_NAME = "kux_launcher.db"
        private const val DATABASE_VERSION = 1

        // Table Name
        const val TABLE_WORKSPACE = "workspace_items"

        // Column Names
        const val COL_ID = "id"
        const val COL_ITEM_TYPE = "item_type"
        const val COL_LABEL = "label"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_CLASS_NAME = "class_name"
        const val COL_CELL_X = "cell_x"
        const val COL_CELL_Y = "cell_y"
        const val COL_CONTAINER = "container"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_WORKSPACE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ITEM_TYPE INTEGER NOT NULL,
                $COL_LABEL TEXT,
                $COL_PACKAGE_NAME TEXT,
                $COL_CLASS_NAME TEXT,
                $COL_CELL_X INTEGER NOT NULL,
                $COL_CELL_Y INTEGER NOT NULL,
                $COL_CONTAINER INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        Log.d(TAG, "Database tables created successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKSPACE")
        onCreate(db)
    }

    /**
     * Loads all workspace items and resolves folders recursively.
     * Keeps execution clean by filtering root items first, then fetching folder children.
     */
    fun getAllWorkspaceItems(): List<WorkspaceItem> {
        val allItems = mutableListOf<WorkspaceItem>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_WORKSPACE,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val folderMap = mutableMapOf<Long, WorkspaceItem>()
        val childrenList = mutableListOf<WorkspaceItem>()

        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndexOrThrow(COL_ID)
            val typeIndex = cursor.getColumnIndexOrThrow(COL_ITEM_TYPE)
            val labelIndex = cursor.getColumnIndexOrThrow(COL_LABEL)
            val packageIndex = cursor.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val classIndex = cursor.getColumnIndexOrThrow(COL_CLASS_NAME)
            val cellXIndex = cursor.getColumnIndexOrThrow(COL_CELL_X)
            val cellYIndex = cursor.getColumnIndexOrThrow(COL_CELL_Y)
            val containerIndex = cursor.getColumnIndexOrThrow(COL_CONTAINER)

            do {
                val id = cursor.getLong(idIndex)
                val itemType = cursor.getInt(typeIndex)
                val label = cursor.getString(labelIndex) ?: ""
                val packageName = cursor.getString(packageIndex)
                val className = cursor.getString(classIndex)
                val cellX = cursor.getInt(cellXIndex)
                val cellY = cursor.getInt(cellYIndex)
                val container = cursor.getInt(containerIndex)

                val item = WorkspaceItem(id, itemType, label, packageName, className, cellX, cellY, container)

                if (container > 0) {
                    // This is a child item inside a folder
                    childrenList.add(item)
                } else {
                    // Root desktop or dock items
                    if (item.isFolder) {
                        folderMap[id] = item
                    }
                    allItems.add(item)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Distribute children items into their respective parent folders
        for (child in childrenList) {
            val parentFolder = folderMap[child.container.toLong()]
            parentFolder?.folderItems?.add(child)
        }

        return allItems
    }

    /**
     * Inserts an item into the database.
     * @return inserted item's generated ID.
     */
    fun insertItem(item: WorkspaceItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ITEM_TYPE, item.itemType)
            put(COL_LABEL, item.label)
            put(COL_PACKAGE_NAME, item.packageName)
            put(COL_CLASS_NAME, item.className)
            put(COL_CELL_X, item.cellX)
            put(COL_CELL_Y, item.cellY)
            put(COL_CONTAINER, item.container)
        }
        val id = db.insert(TABLE_WORKSPACE, null, values)
        Log.d(TAG, "Inserted item: type=${item.itemType}, label=${item.label}, id=$id")
        return id
    }

    /**
     * Updates an item's grid coordinates and container (Dock, Desktop, or Folder).
     */
    fun updateItemPosition(id: Long, cellX: Int, cellY: Int, container: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CELL_X, cellX)
            put(COL_CELL_Y, cellY)
            put(COL_CONTAINER, container)
        }
        db.update(TABLE_WORKSPACE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Updated item position: id=$id, cellX=$cellX, cellY=$cellY, container=$container")
    }

    /**
     * Updates an item's container directly (e.g. adding it into a folder).
     */
    fun updateItemContainer(id: Long, container: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CONTAINER, container)
        }
        db.update(TABLE_WORKSPACE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Updated item container: id=$id, container=$container")
    }

    /**
     * Deletes an item from the database.
     * If the item is a folder, deletes all contained sub-items as well.
     */
    fun deleteItem(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Check if this item is a folder to delete children
            val cursor = db.query(TABLE_WORKSPACE, arrayOf(COL_ITEM_TYPE), "$COL_ID = ?", arrayOf(id.toString()), null, null, null)
            var isFolder = false
            if (cursor.moveToFirst()) {
                val itemType = cursor.getInt(0)
                isFolder = itemType == WorkspaceItem.ITEM_TYPE_FOLDER
            }
            cursor.close()

            if (isFolder) {
                // Delete all items contained inside the folder (where container == id)
                db.delete(TABLE_WORKSPACE, "$COL_CONTAINER = ?", arrayOf(id.toString()))
                Log.d(TAG, "Deleted folder children for folderId=$id")
            }

            // Delete the item itself
            db.delete(TABLE_WORKSPACE, "$COL_ID = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            Log.d(TAG, "Deleted item from DB: id=$id")
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Combines two existing shortcuts into a new Folder item in a transaction-safe manner.
     * @return generated folder ID.
     */
    fun createFolderWithItems(
        folderLabel: String,
        item1: WorkspaceItem,
        item2: WorkspaceItem,
        cellX: Int,
        cellY: Int,
        container: Int
    ): Long {
        val db = writableDatabase
        var folderId: Long = -1
        db.beginTransaction()
        try {
            // 1. Create and insert folder item at target spot
            val folderValues = ContentValues().apply {
                put(COL_ITEM_TYPE, WorkspaceItem.ITEM_TYPE_FOLDER)
                put(COL_LABEL, folderLabel)
                put(COL_CELL_X, cellX)
                put(COL_CELL_Y, cellY)
                put(COL_CONTAINER, container)
            }
            folderId = db.insert(TABLE_WORKSPACE, null, folderValues)

            if (folderId != -1L) {
                // 2. Put original items inside the folder (update container and clear coordinates)
                val itemValues1 = ContentValues().apply {
                    put(COL_CELL_X, 0)
                    put(COL_CELL_Y, 0)
                    put(COL_CONTAINER, folderId.toInt())
                }
                db.update(TABLE_WORKSPACE, itemValues1, "$COL_ID = ?", arrayOf(item1.id.toString()))

                val itemValues2 = ContentValues().apply {
                    put(COL_CELL_X, 0)
                    put(COL_CELL_Y, 0)
                    put(COL_CONTAINER, folderId.toInt())
                }
                db.update(TABLE_WORKSPACE, itemValues2, "$COL_ID = ?", arrayOf(item2.id.toString()))

                db.setTransactionSuccessful()
                Log.d(TAG, "Successfully created Folder '$folderLabel' (id=$folderId) with items: ${item1.label}, ${item2.label}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder", e)
        } finally {
            db.endTransaction()
        }
        return folderId
    }

    /**
     * Rename a folder in the database.
     */
    fun updateFolderLabel(id: Long, newLabel: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_LABEL, newLabel)
        }
        db.update(TABLE_WORKSPACE, values, "$COL_ID = ?", arrayOf(id.toString()))
        Log.d(TAG, "Renamed folder: id=$id, newLabel=$newLabel")
    }
}
