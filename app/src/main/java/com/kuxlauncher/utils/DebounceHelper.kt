package com.kuxlauncher.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lightweight helper to debounce rapid actions (e.g. search queries while typing) inside a CoroutineScope.
 */
class DebounceHelper(private val delayMillis: Long, private val scope: CoroutineScope) {
    private var debounceJob: Job? = null

    /**
     * Debounces the execution of a suspended block of code.
     */
    fun debounce(action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMillis)
            action()
        }
    }

    /**
     * Instantly cancels any pending debounced tasks.
     */
    fun cancel() {
        debounceJob?.cancel()
        debounceJob = null
    }
}
