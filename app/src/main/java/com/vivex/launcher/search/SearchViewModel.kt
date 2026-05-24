package com.vivex.launcher.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vivex.launcher.cache.AppInfoCache
import com.vivex.launcher.data.AppModel
import com.vivex.launcher.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVVM ViewModel driving the real-time debounced Search System for the App Drawer.
 * Offloads indexing and search filtering to background dispatchers to guarantee smooth UI thread operation.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)

    private val _searchResults = MutableLiveData<List<AppModel>>()
    val searchResults: LiveData<List<AppModel>> get() = _searchResults

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> get() = _searchQuery

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> get() = _isSearching

    /**
     * Initializes indexing. Reads from AppInfoCache or triggers AppRepository reload.
     */
    fun initializeIndex(forceReload: Boolean = false) {
        viewModelScope.launch {
            val apps = AppInfoCache.get() ?: withContext(Dispatchers.IO) {
                val loaded = appRepository.getInstalledApps()
                AppInfoCache.set(loaded)
                loaded
            }
            withContext(Dispatchers.Default) {
                SearchIndexer.buildIndex(apps)
            }
            // Populate initial state if search is empty
            if (_searchQuery.value.isNullOrBlank()) {
                _searchResults.postValue(apps)
            }
        }
    }

    /**
     * Executes real-time app search matching.
     * Runs Case-Insensitive filtering inside [Dispatchers.Default] background thread.
     */
    fun performSearch(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotEmpty()

        viewModelScope.launch(Dispatchers.Default) {
            val filtered = SearchIndexer.search(query)
            _searchResults.postValue(filtered)
        }
    }
}
