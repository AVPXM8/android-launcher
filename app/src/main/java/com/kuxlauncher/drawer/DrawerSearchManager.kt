package com.kuxlauncher.drawer

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.LifecycleOwner
import com.kuxlauncher.search.SearchViewModel
import com.kuxlauncher.utils.DebounceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Connects the App Drawer's EditText input field directly to the search MVVM engine.
 * Applies a debounced text watcher to prevent heavy layout recalculations.
 */
class DrawerSearchManager(
    private val searchEditText: EditText,
    private val searchViewModel: SearchViewModel,
    lifecycleOwner: LifecycleOwner
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val debounceHelper = DebounceHelper(delayMillis = 150, scope = scope)

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val query = s?.toString() ?: ""
            debounceHelper.debounce {
                searchViewModel.performSearch(query)
            }
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    init {
        searchEditText.addTextChangedListener(textWatcher)
    }

    /**
     * Resets the search text field and filters.
     */
    fun clearSearch() {
        searchEditText.removeTextChangedListener(textWatcher)
        searchEditText.setText("")
        searchEditText.addTextChangedListener(textWatcher)
        searchViewModel.performSearch("")
    }

    /**
     * Cancels any pending search coroutines to free resources.
     */
    fun onDestroy() {
        searchEditText.removeTextChangedListener(textWatcher)
        scope.cancel()
    }
}
