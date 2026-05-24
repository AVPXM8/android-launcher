package com.kuxlauncher.search

import android.content.ComponentName
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuxlauncher.R
import com.kuxlauncher.cache.IconCache
import com.kuxlauncher.data.AppModel
import com.kuxlauncher.databinding.ItemAppBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Optimized ListAdapter specifically for rendering search results in the app drawer.
 * Utilizes [SearchHighlighter] to dynamically color matching characters.
 */
class SearchAdapter(
    private val packageManager: PackageManager,
    private val onAppClick: (AppModel) -> Unit,
    private val onAppLongClick: (AppModel, View) -> Boolean
) : ListAdapter<AppModel, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {

    private var currentQuery: String = ""

    init {
        setHasStableIds(true)
    }

    /**
     * Updates the query string used for text highlighting.
     */
    fun updateQuery(query: String) {
        currentQuery = query
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).stableId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Retrieve highlight color from the color resource system
        val highlightColor = ContextCompat.getColor(parent.context, R.color.primary_accent)
        return SearchViewHolder(binding, packageManager, highlightColor, onAppClick, onAppLongClick)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position), currentQuery)
    }

    override fun onViewRecycled(holder: SearchViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelIconLoad()
    }

    class SearchViewHolder(
        private val binding: ItemAppBinding,
        private val packageManager: PackageManager,
        private val highlightColor: Int,
        private val onAppClick: (AppModel) -> Unit,
        private val onAppLongClick: (AppModel, View) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private var iconLoadJob: Job? = null

        fun bind(app: AppModel, query: String) {
            // Apply text highlighting
            binding.appLabel.text = SearchHighlighter.highlight(app.label, query, highlightColor)
            
            binding.root.setOnClickListener { onAppClick(app) }
            binding.root.setOnLongClickListener { view -> onAppLongClick(app, view) }

            binding.appIcon.setImageDrawable(null)

            val cachedIcon = IconCache.get(app.packageName)
            if (cachedIcon != null) {
                binding.appIcon.setImageDrawable(cachedIcon)
            } else {
                cancelIconLoad()
                iconLoadJob = CoroutineScope(Dispatchers.Main).launch {
                    val icon = withContext(Dispatchers.IO) {
                        try {
                            val compName = ComponentName(app.packageName, app.className)
                            val info = packageManager.getActivityInfo(compName, 0)
                            val loadedDrawable = info.loadIcon(packageManager)
                            IconCache.put(app.packageName, loadedDrawable)
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

    private class SearchDiffCallback : DiffUtil.ItemCallback<AppModel>() {
        override fun areItemsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem.packageName == newItem.packageName && oldItem.className == newItem.className
        }

        override fun areContentsTheSame(oldItem: AppModel, newItem: AppModel): Boolean {
            return oldItem == newItem
        }
    }
}
