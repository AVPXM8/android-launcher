package com.kuxlauncher.search

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.Locale

/**
 * Text utility to highlight portions of text matching a search query.
 * Produces high-fidelity SpannableString instances for premium visual matching feedback.
 */
object SearchHighlighter {

    /**
     * Highlights the search query within the target text.
     * @param text The full display text of the application (e.g. App Label)
     * @param query The search query to match against
     * @param highlightColor The color integer to paint the matching substring
     * @return Spannable displaying matching substrings in highlighted style
     */
    fun highlight(text: String, query: String, highlightColor: Int): CharSequence {
        if (query.isBlank()) {
            return text
        }

        val spannable = SpannableString(text)
        val normalizedText = text.lowercase(Locale.getDefault())
        val normalizedQuery = query.lowercase(Locale.getDefault())

        var startPos = normalizedText.indexOf(normalizedQuery)
        while (startPos >= 0) {
            val endPos = startPos + normalizedQuery.length
            
            // Highlight color span
            spannable.setSpan(
                ForegroundColorSpan(highlightColor),
                startPos,
                endPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // Extra premium visual: bold the matching characters!
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                startPos,
                endPos,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            startPos = normalizedText.indexOf(normalizedQuery, endPos)
        }

        return spannable
    }
}
