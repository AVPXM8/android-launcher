package com.kuxlauncher.adapter

import android.content.ComponentName
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuxlauncher.cache.IconCache
import com.kuxlauncher.databinding.ItemAppBinding
import com.kuxlauncher.model.WorkspaceItem
import kotlinx.coroutines.*

/**
 * Adapter that renders the grid of items inside a custom launcher Folder.
 * Supports asynchronous icon loading and delegates drag-initiations and clicks.
 */
class FolderAdapter(
    private val packageManager: PackageManager,
    private val onItemClick: (WorkspaceItem) -> Unit,
    private val onItemLongClick: (WorkspaceItem, View) -> Boolean
) : ListAdapter<WorkspaceItem, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding, packageManager, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: FolderViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelIconLoad()
    }

    class FolderViewHolder(
        private val binding: ItemAppBinding,
        private val packageManager: PackageManager,
        private val onItemClick: (WorkspaceItem) -> Unit,
        private val onItemLongClick: (WorkspaceItem, View) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private var iconLoadJob: Job? = null

        fun bind(item: WorkspaceItem) {
            binding.appLabel.text = item.label
            binding.root.setOnClickListener { onItemClick(item) }
            
            // Register long press for dragging apps out of the folder
            binding.root.setOnLongClickListener { view ->
                onItemLongClick(item, view)
            }

            binding.appIcon.setImageDrawable(null)

            val packageName = item.packageName ?: return
            val className = item.className ?: return

            val cachedIcon = IconCache.get(packageName)
            if (cachedIcon != null) {
                binding.appIcon.setImageDrawable(cachedIcon)
            } else {
                cancelIconLoad()
                iconLoadJob = CoroutineScope(Dispatchers.Main).launch {
                    val icon = withContext(Dispatchers.IO) {
                        try {
                            val compName = ComponentName(packageName, className)
                            val info = packageManager.getActivityInfo(compName, 0)
                            val loadedDrawable = info.loadIcon(packageManager)
                            IconCache.put(packageName, loadedDrawable)
                            loadedDrawable
                        } catch (e: Exception) {
                            packageManager.defaultActivityIcon
                        }
                    }
                    binding.appIcon.setImageDrawable(icon)
                }
            }
        }

        fun cancelIconLoad() {
            iconLoadJob?.cancel()
            iconLoadJob = null
        }
    }

    private class FolderDiffCallback : DiffUtil.ItemCallback<WorkspaceItem>() {
        override fun areItemsTheSame(oldItem: WorkspaceItem, newItem: WorkspaceItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkspaceItem, newItem: WorkspaceItem): Boolean {
            return oldItem == newItem
        }
    }
}
