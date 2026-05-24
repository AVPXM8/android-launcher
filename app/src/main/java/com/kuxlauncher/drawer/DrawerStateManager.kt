package com.kuxlauncher.drawer

/**
 * State machine capturing the lifecycle states of the drawer.
 * Tracks and notifies state modifications to prevent layout and gesture conflicts.
 */
class DrawerStateManager(private val onStateChanged: (DrawerState) -> Unit) {

    enum class DrawerState {
        CLOSED,
        DRAGGING,
        ANIMATING,
        OPENED
    }

    private var currentState = DrawerState.CLOSED

    /**
     * Gets the current state of the drawer.
     */
    fun getState(): DrawerState = currentState

    /**
     * Transitions to a new state and invokes the callback if the state changed.
     */
    fun setState(state: DrawerState) {
        if (currentState != state) {
            currentState = state
            onStateChanged(state)
        }
    }

    val isClosed: Boolean get() = currentState == DrawerState.CLOSED
    val isOpened: Boolean get() = currentState == DrawerState.OPENED
    val isDragging: Boolean get() = currentState == DrawerState.DRAGGING
    val isAnimating: Boolean get() = currentState == DrawerState.ANIMATING
}
