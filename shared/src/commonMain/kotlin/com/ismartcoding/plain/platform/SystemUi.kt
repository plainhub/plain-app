package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

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
 * Composable so the Android actual can resolve the host window via LocalView
 * (a Dialog has its own window, separate from the activity).
 */
@Composable
expect fun setImmersiveFullscreen()

/**
 * Exit immersive fullscreen mode and restore system bar behavior to
 * "show transient bars on swipe".
 */
expect fun exitImmersiveFullscreen()

/**
 * Toggle system bar visibility while keeping the edge-to-edge layout intact.
 *
 * Unlike [setImmersiveFullscreen] (which is composable and always hides bars)
 * and [exitImmersiveFullscreen] (which also flips DecorFitsSystemWindows and
 * causes media content to resize), this only flips bar visibility so the
 * media surface never relayouts. Used by media viewers that show/hide bars
 * alongside their control overlays — the top bar's `statusBarsPadding()`
 * stays stable because bars are visible exactly when the top bar is visible.
 *
 * @param visible true to show system bars, false to hide them (immersive).
 */
expect fun setSystemBarsVisible(visible: Boolean)

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

/**
 * Whether the current platform supports Picture-in-Picture mode.
 * Android: checks PackageManager.FEATURE_PICTURE_IN_PICTURE.
 * iOS: always false (PiP not wired up for video preview).
 */
expect fun hasPipMode(): Boolean

/**
 * Attempt to enter Picture-in-Picture mode for the given [videoState].
 * Android: builds PictureInPictureParams and calls enterPictureInPictureMode.
 * iOS: no-op, returns false.
 * @return true if PiP was entered successfully
 */
expect fun enterPipMode(videoState: VideoState): Boolean
