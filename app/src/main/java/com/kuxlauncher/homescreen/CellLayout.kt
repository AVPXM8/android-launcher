package com.kuxlauncher.homescreen

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.kuxlauncher.constants.LauncherConfig

/**
 * A highly performant custom ViewGroup that partitions parent view dimensions into 
 * a perfectly spaced cellular grid (columns x rows) for launcher apps and widgets.
 * Handles measurement and layout of child views based on their grid coordinates (cellX, cellY).
 */
class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var columnCount = LauncherConfig.DEFAULT_COLUMN_COUNT
    private var rowCount = 5

    private var cellWidth = 0
    private var cellHeight = 0

    init {
        // Essential optimization: enables custom view clipping
        setWillNotDraw(false)
    }

    /**
     * Reconfigures the grid size dynamically.
     */
    fun setGridSize(columns: Int, rows: Int) {
        if (this.columnCount != columns || this.rowCount != rows) {
            this.columnCount = columns
            this.rowCount = rows
            requestLayout()
        }
    }

    fun getColumnCount() = columnCount
    fun getRowCount() = rowCount

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val totalPaddingLeft = paddingLeft
        val totalPaddingRight = paddingRight
        val totalPaddingTop = paddingTop
        val totalPaddingBottom = paddingBottom

        val availableWidth = widthSize - totalPaddingLeft - totalPaddingRight
        val availableHeight = heightSize - totalPaddingTop - totalPaddingBottom

        cellWidth = if (columnCount > 0) availableWidth / columnCount else availableWidth
        cellHeight = if (rowCount > 0) availableHeight / rowCount else availableHeight

        // Measure children and assign their dimensions precisely to cell dimensions
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as LayoutParams
                
                // Measure child with exact width and height of one grid cell
                val childWidthSpec = MeasureSpec.makeMeasureSpec(cellWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY)
                val childHeightSpec = MeasureSpec.makeMeasureSpec(cellHeight - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY)
                
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as LayoutParams
                
                // Calculate pixel coordinates based on layout parameter cell index
                val childLeft = paddingLeft + (lp.cellX * cellWidth) + lp.leftMargin
                val childTop = paddingTop + (lp.cellY * cellHeight) + lp.topMargin
                val childRight = childLeft + child.measuredWidth
                val childBottom = childTop + child.measuredHeight
                
                child.layout(childLeft, childTop, childRight, childBottom)
            }
        }
    }

    /**
     * Converts a raw screen pixel location (x, y) relative to this view into cell coordinates.
     */
    fun pointToCell(x: Float, y: Float, result: IntArray) {
        val relativeX = x - paddingLeft
        val relativeY = y - paddingTop

        var cellX = (relativeX / cellWidth).toInt()
        var cellY = (relativeY / cellHeight).toInt()

        // Constrain to grid boundaries
        cellX = cellX.coerceIn(0, columnCount - 1)
        cellY = cellY.coerceIn(0, rowCount - 1)

        result[0] = cellX
        result[1] = cellY
    }

    /**
     * Finds a child view that occupies the specified cell coordinates.
     */
    fun findChildAtCell(cellX: Int, cellY: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutParams
            if (lp.cellX == cellX && lp.cellY == cellY) {
                return child
            }
        }
        return null
    }

    /**
     * Scans the grid for the nearest vacant cell around a dropped coordinate.
     * @return True if a vacant cell is found and outputted in [result], false otherwise.
     */
    fun findNearestVacantCell(targetX: Int, targetY: Int, result: IntArray): Boolean {
        // Start scanning in concentric squares radiating outward from target
        val maxDist = Math.max(columnCount, rowCount)
        for (dist in 0..maxDist) {
            for (dx in -dist..dist) {
                for (dy in -dist..dist) {
                    // Only check cells on boundary of current distance square
                    if (Math.abs(dx) == dist || Math.abs(dy) == dist) {
                        val cx = targetX + dx
                        val cy = targetY + dy
                        if (cx in 0 until columnCount && cy in 0 until rowCount) {
                            if (findChildAtCell(cx, cy) == null) {
                                result[0] = cx
                                result[1] = cy
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    // --- LayoutParams overrides to support standard XML inflation and customized child params ---

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    /**
     * Custom LayoutParams holding cellular cellX and cellY positions for child layout calculation.
     */
    class LayoutParams : MarginLayoutParams {
        var cellX: Int = 0
        var cellY: Int = 0

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            // Can be extended to read app:cellX / app:cellY if styled attributes are declared.
        }

        constructor(width: Int, height: Int) : super(width, height)
        
        constructor(width: Int, height: Int, cellX: Int, cellY: Int) : super(width, height) {
            this.cellX = cellX
            this.cellY = cellY
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }
}
