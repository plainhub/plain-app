package com.ismartcoding.plain.platform

/**
 * Whether the device uses gesture navigation (as opposed to 3-button nav).
 * On iOS this always returns true (no legacy nav bar mode).
 */
expect fun isGestureInteractionMode(): Boolean

/**
 * Add or remove the KEEP_SCREEN_ON flag on the current window.
 */
expect fun keepScreenOn(enabled: Boolean)

/**
 * Hide the system navigation bar for the current window.
 */
expect fun hideNavigationBar()

/**
 * Show the system navigation bar for the current window.
 */
expect fun showNavigationBar()

/**
 * Enter immersive fullscreen mode (hides system bars, edge-to-edge layout).
 * Should be called from a SideEffect/DisposableEffect scoped to a Dialog/Window.
 */
expect fun setImmersiveFullscreen()

/**
 * Exit immersive fullscreen mode and restore system bar behavior to
 * "show transient bars on swipe".
 */
expect fun exitImmersiveFullscreen()
