package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication

actual fun isGestureInteractionMode(): Boolean = true

actual fun keepScreenOn(enabled: Boolean) {
    UIApplication.sharedApplication.setIdleTimerDisabled(enabled)
}

actual fun hideNavigationBar() {
    // iOS has no legacy navigation bar; controlled via Compose safe areas.
}

actual fun showNavigationBar() {
    // iOS has no legacy navigation bar; controlled via Compose safe areas.
}

@Composable
actual fun setImmersiveFullscreen() {
    // iOS immersive mode handled via UIViewController prefersStatusBarHidden
    // and Compose safe areas — no global window flag equivalent.
}

actual fun exitImmersiveFullscreen() {
    // iOS immersive mode handled via UIViewController prefersStatusBarHidden.
}

@Composable
actual fun rememberWindowInsetsController(): Any = Unit

actual fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean) {
    // iOS: status bar appearance is controlled via UIViewController.preferredStatusBarStyle.
}
