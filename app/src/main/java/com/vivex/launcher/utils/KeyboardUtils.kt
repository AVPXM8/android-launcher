package com.vivex.launcher.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Utility class to robustly show and hide the soft keyboard, providing support
 * for both modern WindowInsetsController APIs and legacy InputMethodManager.
 */
object KeyboardUtils {

    /**
     * Focuses the target view and opens the software keyboard.
     */
    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Closes the software keyboard and clears view focus.
     */
    fun hideKeyboard(view: View) {
        view.clearFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
