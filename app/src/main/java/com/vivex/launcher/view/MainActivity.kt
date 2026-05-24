package com.vivex.launcher.view

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.StrictMode
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.vivex.launcher.R
import com.vivex.launcher.cache.IconCache
import com.vivex.launcher.databinding.ActivityMainBinding
import com.vivex.launcher.databinding.ItemAppBinding
import com.vivex.launcher.drawer.AppDrawerFragment
import com.vivex.launcher.gesture.SwipeGestureDetector
import com.vivex.launcher.gesture.WorkspaceDragListener
import com.vivex.launcher.homescreen.CellLayout
import com.vivex.launcher.homescreen.FolderPopup
import com.vivex.launcher.model.WorkspaceItem
import com.vivex.launcher.utils.DragHelper
import com.vivex.launcher.utils.DragObject
import com.vivex.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The central orchestrator activity of ViveX Launcher Phase 2.
 * Connects the persistent Workspace grid, the persistent Dock grid, the dynamic Drag-and-Drop
 * controller, the sliding BottomSheet App Drawer, and folder interaction overlays.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LauncherViewModel by viewModels()
    
    private val drawerFragment: AppDrawerFragment
        get() = supportFragmentManager.findFragmentById(R.id.drawerContainer) as AppDrawerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setupStrictModeIfDebug()
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initialize custom cellular grids and drag-drop zones
        setupWorkspaceAndDock()

        // 2. Setup standard gesture controllers and drag-drop callbacks for the App Drawer
        setupAppDrawerCallbacks()

        // 3. Intercept back button gestures to control the sliding drawer
        setupBackPressedDispatcher()

        // 4. Connect LiveData observations
        observeViewModel()

        // 5. Query data asynchronously
        viewModel.loadApps()
        viewModel.loadWorkspaceItems()
    }

    /**
     * Configures the grid bounds of the Workspace and the persistence Dock,
     * and installs their custom [WorkspaceDragListener] engines.
     */
    private fun setupWorkspaceAndDock() {
        // Set workspace grid to 5 columns x 5 rows
        binding.workspaceGrid.setGridSize(5, 5)

        // Set dock grid to 5 columns x 1 row
        binding.dockGrid.setGridSize(5, 1)

        // Setup drop callbacks for the Desktop Workspace
        val workspaceDragListener = WorkspaceDragListener(
            containerType = WorkspaceItem.CONTAINER_DESKTOP,
            onDropToEmptyCell = { dragged, cellX, cellY, container ->
                handleDropToEmptyCell(dragged, cellX, cellY, container)
            },
            onDropToShortcut = { dragged, target, cellX, cellY, container ->
                handleDropToShortcut(dragged, target, cellX, cellY, container)
            },
            onDropToFolder = { dragged, folder ->
                handleDropToFolder(dragged, folder)
            }
        )
        binding.workspaceGrid.setOnDragListener(workspaceDragListener)

        // Setup drop callbacks for the persistent Dock
        val dockDragListener = WorkspaceDragListener(
            containerType = WorkspaceItem.CONTAINER_DOCK,
            onDropToEmptyCell = { dragged, cellX, cellY, container ->
                handleDropToEmptyCell(dragged, cellX, cellY, container)
            },
            onDropToShortcut = { dragged, target, cellX, cellY, container ->
                handleDropToShortcut(dragged, target, cellX, cellY, container)
            },
            onDropToFolder = { dragged, folder ->
                handleDropToFolder(dragged, folder)
            }
        )
        binding.dockGrid.setOnDragListener(dockDragListener)

        // Setup Drag Remove Zone at the top of the screen
        setupRemoveZoneDragListener()
    }

    /**
     * Sets up a drag listener on the "Remove Zone" CardView at the top.
     * Shows the zone during active drag operations, hides it when completed,
     * and deletes workspace shortcuts dropped within it.
     */
    private fun setupRemoveZoneDragListener() {
        binding.removeZone.setOnDragListener { _, event ->
            val dragObject = event.localState as? DragObject ?: return@setOnDragListener false
            
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Reveal the remove zone visually when any drag starts
                    binding.removeZone.visibility = View.VISIBLE
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    // Hide the remove zone when dragging is complete
                    binding.removeZone.visibility = View.GONE
                    true
                }
                DragEvent.ACTION_DROP -> {
                    // Delete item if it was dragged from the home screen
                    if (!dragObject.isDrawerDrag && dragObject.workspaceItem != null) {
                        viewModel.removeItem(dragObject.workspaceItem)
                        Toast.makeText(this, "Shortcut removed", Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        Toast.makeText(this, "Cannot remove drawer apps", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> true
            }
        }
    }

    /**
     * Connects upward gestural swipes to reveal our new drawer fragment,
     * and sets drag-and-drop callbacks for when drawer items are lifted.
     */
    private fun setupAppDrawerCallbacks() {
        val swipeGestureDetector = SwipeGestureDetector(
            context = this,
            onSwipeUp = {
                drawerFragment.open()
            },
            onSwipeDown = {
                drawerFragment.close()
            }
        )
        binding.workspaceContainer.setOnTouchListener(swipeGestureDetector)

        drawerFragment.onAppLongClickCallback = { app, itemView ->
            // Lift app icon and start native Android drag operation
            val dragObject = DragObject(isDrawerDrag = true, appModel = app, draggedView = itemView)
            DragHelper.startDrag(itemView, dragObject)
        }
    }

    /**
     * Handles intercepting the system back press.
     * Collapses the app drawer sheet if it is expanded, instead of closing the application.
     */
    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerFragment.isOpened()) {
                    drawerFragment.close()
                } else {
                    // Do nothing (default launcher behavior: lock focus on home desktop)
                }
            }
        })
    }

    /**
     * Observes ViewModel live updates for app drawer lists and persistent cell structures.
     */
    private fun observeViewModel() {
        viewModel.workspaceItems.observe(this) { items ->
            renderWorkspace(items)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingProgressBar.isVisible = isLoading
        }
    }

    /**
     * Dynamically renders workspace shortcuts and folders inside their respective [CellLayout] grid targets.
     */
    private fun renderWorkspace(items: List<WorkspaceItem>) {
        binding.workspaceGrid.removeAllViews()
        binding.dockGrid.removeAllViews()

        for (item in items) {
            // Folders are processed as single parent items on the desktop level.
            // Items with a positive container ID are inside a folder, so skip direct desktop binding.
            if (item.isInFolder) continue

            val bindingApp = ItemAppBinding.inflate(LayoutInflater.from(this), null, false)
            val itemView = bindingApp.root
            itemView.tag = item // Attach workspace item data structure for drag listeners

            // Define CellLayout layout bounds
            val cellLp = CellLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                item.cellX,
                item.cellY
            )
            itemView.layoutParams = cellLp

            if (item.isFolder) {
                // RENDER AS FOLDER
                bindingApp.appLabel.text = item.label
                bindingApp.appIcon.setImageResource(R.drawable.ic_folder)
                bindingApp.appIcon.setBackgroundResource(R.drawable.bg_folder_icon)
                bindingApp.appIcon.setPadding(12, 12, 12, 12)

                itemView.setOnClickListener {
                    showFolderPopup(item)
                }

                itemView.setOnLongClickListener { view ->
                    val dragObject = DragObject(isDrawerDrag = false, workspaceItem = item, draggedView = view)
                    DragHelper.startDrag(view, dragObject)
                    true
                }
            } else {
                // RENDER AS STANDARD APP SHORTCUT
                bindingApp.appLabel.text = item.label
                bindingApp.appIcon.setPadding(0, 0, 0, 0)
                bindingApp.appIcon.setBackground(null)

                val packageName = item.packageName ?: ""
                val className = item.className ?: ""

                // Load icon from thread-safe LruCache
                val cached = IconCache.get(packageName)
                if (cached != null) {
                    bindingApp.appIcon.setImageDrawable(cached)
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        val drawable = withContext(Dispatchers.IO) {
                            try {
                                val comp = ComponentName(packageName, className)
                                val info = packageManager.getActivityInfo(comp, 0)
                                val loaded = info.loadIcon(packageManager)
                                IconCache.put(packageName, loaded)
                                loaded
                            } catch (e: Exception) {
                                packageManager.defaultActivityIcon
                            }
                        }
                        bindingApp.appIcon.setImageDrawable(drawable)
                    }
                }

                itemView.setOnClickListener {
                    launchApp(packageName, className, item.label)
                }

                itemView.setOnLongClickListener { view ->
                    val dragObject = DragObject(isDrawerDrag = false, workspaceItem = item, draggedView = view)
                    DragHelper.startDrag(view, dragObject)
                    true
                }
            }

            // Route view to correct cell container layer
            if (item.container == WorkspaceItem.CONTAINER_DOCK) {
                binding.dockGrid.addView(itemView)
            } else {
                binding.workspaceGrid.addView(itemView)
            }
        }
    }

    /**
     * Opens the circular glassmorphic folder overlay modal.
     */
    private fun showFolderPopup(folderItem: WorkspaceItem) {
        val popup = FolderPopup(
            context = this,
            folderItem = folderItem,
            onAppClick = { childItem ->
                launchApp(childItem.packageName ?: "", childItem.className ?: "", childItem.label)
            },
            onAppDragOut = { draggedChild, childView ->
                // Drag out of folder: create standard drag object.
                // Triggers updates upon dropping onto Desktop Workspace or Dock.
                val dragObject = DragObject(isDrawerDrag = false, workspaceItem = draggedChild, draggedView = childView)
                
                // Lift original child and launch drag sequence!
                DragHelper.startDrag(binding.workspaceContainer, dragObject)
            },
            onFolderRename = { folderId, newName ->
                viewModel.renameFolder(folderId, newName)
            }
        )
        popup.show()
    }

    // --- Drag and Drop Drop-Target Callback Routers ---

    private fun handleDropToEmptyCell(dragged: DragObject, cellX: Int, cellY: Int, container: Int) {
        if (dragged.isDrawerDrag && dragged.appModel != null) {
            // Dragged app from drawer to empty desktop cell -> Create shortcut
            viewModel.addShortcutToWorkspace(dragged.appModel, cellX, cellY, container)
            Toast.makeText(this, "Shortcut placed", Toast.LENGTH_SHORT).show()
        } else if (!dragged.isDrawerDrag && dragged.workspaceItem != null) {
            // Dragged existing desktop item to empty desktop cell -> Move position
            viewModel.updateItemPosition(dragged.workspaceItem, cellX, cellY, container)
        }
    }

    private fun handleDropToShortcut(
        dragged: DragObject,
        target: WorkspaceItem,
        cellX: Int,
        cellY: Int,
        container: Int
    ) {
        if (dragged.isDrawerDrag && dragged.appModel != null) {
            // Drop drawer app onto existing desktop shortcut -> Create folder with both
            viewModel.mergeDrawerAppWithShortcut(dragged.appModel, target, cellX, cellY, container)
            Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
        } else if (!dragged.isDrawerDrag && dragged.workspaceItem != null) {
            // Ensure you cannot drop a shortcut onto itself or drop folders onto shortcuts to merge
            if (dragged.workspaceItem.id != target.id && !dragged.workspaceItem.isFolder) {
                viewModel.mergeShortcutsToFolder(dragged.workspaceItem, target, cellX, cellY, container)
                Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDropToFolder(dragged: DragObject, folder: WorkspaceItem) {
        if (dragged.isDrawerDrag && dragged.appModel != null) {
            // Drop drawer app onto folder -> Add it inside the folder
            viewModel.addDrawerAppToFolder(dragged.appModel, folder)
            Toast.makeText(this, "Added to folder", Toast.LENGTH_SHORT).show()
        } else if (!dragged.isDrawerDrag && dragged.workspaceItem != null) {
            // Ensure you are not dropping the folder inside itself
            if (dragged.workspaceItem.id != folder.id && !dragged.workspaceItem.isFolder) {
                viewModel.addShortcutToFolder(dragged.workspaceItem, folder)
                Toast.makeText(this, "Added to folder", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Intent Launchers and System Visual Safety hooks ---

    /**
     * Public delegate to launch an application from the app drawer.
     */
    fun launchAppDirect(packageName: String, className: String, label: String) {
        launchApp(packageName, className, label)
    }

    private fun launchApp(packageName: String, className: String, label: String) {
        val compName = ComponentName(packageName, className)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            component = compName
        }

        if (launchIntent != null) {
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open $label", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Application cannot be launched directly", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStrictModeIfDebug() {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
