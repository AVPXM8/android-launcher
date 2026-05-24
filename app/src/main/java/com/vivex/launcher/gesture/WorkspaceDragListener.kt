package com.vivex.launcher.gesture

import android.view.DragEvent
import android.view.View
import com.vivex.launcher.homescreen.CellLayout
import com.vivex.launcher.model.WorkspaceItem
import com.vivex.launcher.utils.DragObject

/**
 * Custom DragListener that acts as the controller for launcher drop zones (Workspace & Dock).
 * Translates raw drag pixels to grid cells, detects cell availability, and handles
 * shortcut positioning, folder merges, and invalid drag recovery.
 */
class WorkspaceDragListener(
    private val containerType: Int, // WorkspaceItem.CONTAINER_DESKTOP or CONTAINER_DOCK
    private val onDropToEmptyCell: (dragged: DragObject, cellX: Int, cellY: Int, container: Int) -> Unit,
    private val onDropToShortcut: (dragged: DragObject, target: WorkspaceItem, cellX: Int, cellY: Int, container: Int) -> Unit,
    private val onDropToFolder: (dragged: DragObject, folder: WorkspaceItem) -> Unit
) : View.OnDragListener {

    private val tempCell = IntArray(2)

    override fun onDrag(v: View, event: DragEvent): Boolean {
        val cellLayout = v as? CellLayout ?: return false

        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // Retrieve the drag object and return true to accept drags of standard launcher types
                val dragObject = event.localState as? DragObject
                return dragObject != null
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                // Visual feedback: can set alpha or stroke color to show active drag hover
                cellLayout.alpha = 0.95f
                return true
            }

            DragEvent.ACTION_DRAG_LOCATION -> {
                // Dynamically map drag coordinates to cell layout indexes
                cellLayout.pointToCell(event.x, event.y, tempCell)
                return true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                cellLayout.alpha = 1.0f
                return true
            }

            DragEvent.ACTION_DROP -> {
                cellLayout.alpha = 1.0f
                val dragObject = event.localState as? DragObject ?: return false
                
                cellLayout.pointToCell(event.x, event.y, tempCell)
                val targetCellX = tempCell[0]
                val targetCellY = tempCell[1]

                val targetChildView = cellLayout.findChildAtCell(targetCellX, targetCellY)

                if (targetChildView == null) {
                    // Cell is vacant: place the item directly
                    onDropToEmptyCell(dragObject, targetCellX, targetCellY, containerType)
                    return true
                } else {
                    // Cell is occupied: resolve whether we should merge or displace
                    val targetItem = targetChildView.tag as? WorkspaceItem
                    if (targetItem != null) {
                        // Prevent merging folder inside another folder or folder onto shortcut
                        val isDraggedItemFolder = dragObject.workspaceItem?.isFolder == true

                        if (targetItem.isFolder) {
                            if (!isDraggedItemFolder) {
                                // Add shortcut into folder
                                onDropToFolder(dragObject, targetItem)
                                return true
                            }
                        } else {
                            if (!isDraggedItemFolder) {
                                // Create new folder out of the two shortcuts
                                onDropToShortcut(dragObject, targetItem, targetCellX, targetCellY, containerType)
                                return true
                            }
                        }
                    }

                    // Fallback: If cell is occupied and we can't merge, search for nearest vacant cell
                    val vacantFound = cellLayout.findNearestVacantCell(targetCellX, targetCellY, tempCell)
                    if (vacantFound) {
                        onDropToEmptyCell(dragObject, tempCell[0], tempCell[1], containerType)
                        return true
                    }
                }
                return false
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                cellLayout.alpha = 1.0f
                val dragObject = event.localState as? DragObject
                
                // CRITICAL SAFETY HOOK: If drag failed (was dropped in an invalid area or rejected),
                // make the source view visible again so the icon doesn't vanish from the screen.
                if (!event.result) {
                    dragObject?.draggedView?.let { sourceView ->
                        sourceView.post { sourceView.visibility = View.VISIBLE }
                    }
                }
                return true
            }
        }
        return false
    }
}
