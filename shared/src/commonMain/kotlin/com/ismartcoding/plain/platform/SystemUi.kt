package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable

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

/**
 * Returns the platform's window insets controller for the current window.
 * On Android this returns a `WindowInsetsControllerCompat`; on iOS it returns `Unit`.
 */
@Composable
expect fun rememberWindowInsetsController(): Any

/**
 * Apply the platform's system bar appearance based on the dark theme state.
 * On Android this sets the light/dark appearance of the status & navigation
 * bar icons and disables navigation bar contrast in gesture nav mode.
 * On iOS this is a no-op (status bar appearance is controlled per-UIViewController).
 */
expect fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean)
