package com.kuxlauncher.adapter

import android.content.ComponentName
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuxlauncher.cache.IconCache
import com.kuxlauncher.data.AppModel
import com.kuxlauncher.databinding.ItemAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.View

/**
 * A highly optimized ListAdapter that leverages DiffUtil to calculate list changes on a background thread.
 * Enables Stable IDs for ultra-smooth list scrolling and binds app icons asynchronously to avoid UI stutter.
 */
class AppAdapter(
    private val packageManager: PackageManager,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel, View) -> Boolean
) : ListAdapter<AppModel, AppAdapter.AppViewHolder>(AppDiffCallback()) {

    init {
        // Essential optimization: enables RecyclerView to reuse views perfectly, avoiding duplicate drawing
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).stableId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding, packageManager, onAppClick, onAppLongClick)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        super.onViewRecycled(holder)
        // Avoid leaking tasks or running jobs for recycled views
        holder.cancelIconLoad()
    }

    /**
     * ViewHolder wrapping the item layout and holding references to active async loading jobs.
     */
    class AppViewHolder(
        private val binding: ItemAppBinding,
        private val packageManager: PackageManager,
        private val onAppClick: (AppModel) -> Unit,
        private val onAppLongClick: (AppModel, View) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private var iconLoadJob: Job? = null

        /**
         * Binds the application data to the layout view bindings.
         */
        fun bind(app: AppModel) {
            binding.appLabel.text = app.label
            binding.root.setOnClickListener { onAppClick(app) }
            
            // Wire up the long-click listener to allow dragging from the app drawer
            binding.root.setOnLongClickListener { view ->
                onAppLongClick(app, view)
            }

            // Clear standard placeholder/previous recycled icon to prevent flash
            binding.appIcon.setImageDrawable(null)

            val cachedIcon = IconCache.get(app.packageName)
            if (cachedIcon != null) {
                // Instantly apply cached drawable (synchronous)
                binding.appIcon.setImageDrawable(cachedIcon)
            } else {
                // Fetch the drawable asynchronously on background thread if cache missed
                cancelIconLoad()
                
                // Launching standard coroutine to retrieve icon safely without blocking main thread
                iconLoadJob = CoroutineScope(Dispatchers.Main).launch {
                    val icon = withContext(Dispatchers.IO) {
                        try {
                            val compName = ComponentName(app.packageName, app.className)
                            val info = packageManager.getActivityInfo(compName, 0)
                            val loadedDrawable = info.loadIcon(packageManager)
                            
                            // Cache it in LruCache for subsequent loads
                            IconCache.put(app.packageName, loadedDrawable)
                            loadedDrawable
                        } catch (e: Exception) {
                            // Fallback to default application system icon
                            packageManager.defaultActivityIcon
                        }
                    }
                    // Apply to ImageView on Main dispatcher thread safely
                    binding.appIcon.setImageDrawable(icon)
                }
            }
        }

        /**
         * Cancels the active icon loading coroutine job to free resources and avoid incorrect bindings.
         */
        fun cancelIconLoad() {
            iconLoadJob?.cancel()
            iconLoadJob = null
        }
    }

    /**
     * Efficient item comparison callback utilizing lightweight AppModel values.
     */
    private class AppDiffCallback : DiffUtil.ItemCallback<AppModel>() {
        override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem.packageName == newItem.packageName && oldItem.className == newItem.className
        }

        override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem == newItem
        }
    }
}
