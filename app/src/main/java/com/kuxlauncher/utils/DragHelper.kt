package com.kuxlauncher.utils

import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.view.View
import com.kuxlauncher.data.AppModel
import com.kuxlauncher.model.WorkspaceItem

/**
 * Rich model representing an active launcher drag operation.
 * Holds references to the origin state (Drawer vs Workspace) and the dragged View.
 */
data class DragObject(
    val isDrawerDrag: Boolean,
    val appModel: AppModel? = null,
    val workspaceItem: WorkspaceItem? = null,
    val draggedView: View? = null
)

/**
 * Utility helper to initiate smooth native Android Drag-and-Drop sessions.
 * Configures dynamic visual feedback scale and passes state safely.
 */
object DragHelper {

    /**
     * Start dragging a launcher shortcut item.
     * Uses [View.startDragAndDrop] with custom [LauncherDragShadowBuilder] for a premium, scaled-up look.
     */
    fun startDrag(view: View, dragObject: DragObject) {
        val clipItem = ClipData.Item("KuxDragItem")
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val clipData = ClipData("LauncherDragData", mimeTypes, clipItem)

        // Custom shadow builder for scaled-up "lifted" drag effect
        val shadowBuilder = LauncherDragShadowBuilder(view)

        val flags = View.DRAG_FLAG_OPAQUE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            view.startDragAndDrop(clipData, shadowBuilder, dragObject, flags)
        } else {
            @Suppress("DEPRECATION")
            view.startDrag(clipData, shadowBuilder, dragObject, flags)
        }

        // Lift view: hide the source desktop shortcut during workspace rearrangement
        if (!dragObject.isDrawerDrag) {
            view.visibility = View.INVISIBLE
        }
    }

    /**
     * A premium drag shadow builder that scales up and adds transparency to the dragged icon,
     * mimicking the high-quality layout-lifting feel of the Google Pixel launcher.
     */
    private class LauncherDragShadowBuilder(view: View) : View.DragShadowBuilder(view) {
        
        override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
            val width = view.width
            val height = view.height

            // Scale up the shadow size by 15% to create a "lifting off the screen" depth effect
            val scaledWidth = (width * 1.15f).toInt()
            val scaledHeight = (height * 1.15f).toInt()

            outShadowSize.set(scaledWidth, scaledHeight)
            
            // Keep the touch hotspot directly in the center of the scaled shadow
            outShadowTouchPoint.set(scaledWidth / 2, scaledHeight / 2)
        }

        override fun onDrawShadow(canvas: Canvas) {
            val view = view ?: return
            
            // Draw slightly larger
            canvas.save()
            canvas.scale(1.15f, 1.15f)
            view.draw(canvas)
            canvas.restore()
        }
    }
}
