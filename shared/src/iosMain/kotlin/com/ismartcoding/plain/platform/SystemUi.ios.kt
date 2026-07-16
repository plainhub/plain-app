package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable

actual fun isGestureInteractionMode(): Boolean = true

actual fun keepScreenOn(enabled: Boolean) {
    // iOS uses UIApplication.idleTimerDisabled; wired in Phase 25
}

actual fun hideNavigationBar() {
    // No legacy navigation bar on iOS
}

actual fun showNavigationBar() {
    // No legacy navigation bar on iOS
}

actual fun setImmersiveFullscreen() {
    // iOS immersive mode handled via UIViewController prefersStatusBarHidden
}

actual fun exitImmersiveFullscreen() {
    // No-op on iOS
}

@Composable
actual fun rememberWindowInsetsController(): Any = Unit

actual fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean) {
    // iOS: status bar appearance is controlled via UIViewController.preferredStatusBarStyle
}
