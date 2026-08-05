package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
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
    // Tell Swift to create a PassThroughWindow (above all other windows,
    // including the Compose Dialog window) whose root VC hides the Status Bar
    // and Home Indicator. The window's hitTest returns nil so touches pass
    // through to the Compose Dialog below.
    SideEffect {
        IosPlatformRegistry.setImmersive(enabled = true)
    }
}

actual fun exitImmersiveFullscreen() {
    // Dismiss the PassThroughWindow and restore the previous key window.
    IosPlatformRegistry.setImmersive(enabled = false)
}

actual fun setSystemBarsVisible(visible: Boolean) {
    // Mirror the immersive toggle: visible bars => not immersive.
    IosPlatformRegistry.setImmersive(enabled = !visible)
}

@Composable
actual fun rememberWindowInsetsController(): Any = Unit

actual fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean) {
    // iOS: status bar appearance is controlled via UIViewController.preferredStatusBarStyle.
    // The ComposeHostingController (Swift) handles immersive toggling; normal
    // light/dark status bar styling is left to Compose Multiplatform defaults.
}

actual fun hasPipMode(): Boolean = false

actual fun enterPipMode(videoState: VideoState): Boolean = false
