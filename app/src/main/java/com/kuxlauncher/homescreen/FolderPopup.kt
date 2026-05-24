package com.kuxlauncher.homescreen

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.GridLayoutManager
import com.kuxlauncher.adapter.FolderAdapter
import com.kuxlauncher.databinding.PopupFolderBinding
import com.kuxlauncher.model.WorkspaceItem

/**
 * Custom dialog that handles visual details, renames, and drag-out interactions 
 * for folder items placed on the home screen.
 */
class FolderPopup(
    private val context: Context,
    private val folderItem: WorkspaceItem,
    private val onAppClick: (WorkspaceItem) -> Unit,
    private val onAppDragOut: (WorkspaceItem, View) -> Unit,
    private val onFolderRename: (Long, String) -> Unit
) {

    private val dialog = Dialog(context)
    private val binding: PopupFolderBinding

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = PopupFolderBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // Make dialog window background transparent so CardView round corners display beautifully
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // Premium layout parameters
            val lp = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            window.attributes = lp
        }

        setupUI()
    }

    private fun setupUI() {
        // Setup title editing
        binding.folderTitle.setText(folderItem.label)
        binding.folderTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveRename()
                true
            } else {
                false
            }
        }
        binding.folderTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveRename()
            }
        }

        // Setup grid of inside shortcuts
        val adapter = FolderAdapter(
            context.packageManager,
            onItemClick = { clickedItem ->
                dialog.dismiss()
                onAppClick(clickedItem)
            },
            onItemLongClick = { draggedItem, itemView ->
                // Dismiss the dialog immediately to let user drop the shortcut on the desktop workspace grid!
                dialog.dismiss()
                onAppDragOut(draggedItem, itemView)
                true
            }
        )

        binding.folderItemsGrid.layoutManager = GridLayoutManager(context, 4)
        binding.folderItemsGrid.adapter = adapter
        adapter.submitList(folderItem.folderItems)
    }

    private fun saveRename() {
        val newTitle = binding.folderTitle.text.toString().trim()
        if (newTitle.isNotEmpty() && newTitle != folderItem.label) {
            folderItem.label = newTitle
            onFolderRename(folderItem.id, newTitle)
        }
        binding.folderTitle.clearFocus()
    }

    /**
     * Display the folder modal overlay.
     */
    fun show() {
        dialog.show()
    }

    /**
     * Programmatically dismiss the folder modal overlay.
     */
    fun dismiss() {
        dialog.dismiss()
    }
}
