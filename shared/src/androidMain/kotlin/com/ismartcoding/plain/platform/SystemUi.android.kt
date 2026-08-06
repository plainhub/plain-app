package com.ismartcoding.plain.platform

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

actual fun isGestureInteractionMode(): Boolean {
    val context = com.ismartcoding.plain.appContextValue ?: return false
    return context.isGestureInteractionMode()
}

actual fun keepScreenOn(enabled: Boolean) {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    if (enabled) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

actual fun hideNavigationBar() {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    val view = activity.window.decorView
    WindowCompat.getInsetsController(activity.window, view)
        .hide(WindowInsetsCompat.Type.navigationBars())
}

actual fun showNavigationBar() {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    val view = activity.window.decorView
    WindowCompat.getInsetsController(activity.window, view)
        .show(WindowInsetsCompat.Type.navigationBars())
}

@Composable
actual fun setImmersiveFullscreen() {
    val view = LocalView.current
    val activity = LocalActivity.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
            ?: (activity as? Activity)?.window
            ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

actual fun exitImmersiveFullscreen() {
    val activity = com.ismartcoding.plain.mainActivity ?: return
    // Keep DecorFitsSystemWindows = false to match MainActivity's default
    // (setDecorFitsSystemWindows(window, false) in MainActivity.onCreate).
    // Flipping it to true would trigger a full Activity relayout, causing a
    // visible flash on Huawei devices. We only need to restore bar visibility.
    WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
        show(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

actual fun setSystemBarsVisible(visible: Boolean) {
    val activity = com.ismartcoding.plain.mainActivity ?: return
    // Keep edge-to-edge so the media surface never relayouts when bars toggle.
    // Only toggle the status bar — the navigation bar (gesture bar / 3-button
    // bar) stays hidden so the bottom of the player never shows a system bar
    // when the control overlay appears.
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    controller.hide(WindowInsetsCompat.Type.navigationBars())
    if (visible) {
        controller.show(WindowInsetsCompat.Type.statusBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
actual fun rememberWindowInsetsController(): Any {
    val window = with(LocalActivity.current as Activity) { return@with window }
    return remember { WindowCompat.getInsetsController(window, window.decorView) }
}

actual fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean) {
    val activity = com.ismartcoding.plain.mainActivity ?: return
    val window = activity.window
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !useDarkTheme
        isAppearanceLightNavigationBars = !useDarkTheme
    }
    if (isQPlus() && isGestureInteractionMode()) {
        window.isNavigationBarContrastEnforced = false
    }
}

actual fun hasPipMode(): Boolean {
    val context = com.ismartcoding.plain.appContextValue ?: return false
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

actual fun enterPipMode(videoState: VideoState): Boolean {
    val context = com.ismartcoding.plain.mainActivity ?: return false
    if (!hasPipMode()) return false
    videoState.enablePip = true
    val params = PictureInPictureParams.Builder()
    if (isTPlus()) {
        params
            .setTitle("Video Player")
            .setAspectRatio(Rational(16, 9))
            .setSeamlessResizeEnabled(true)
    }
    return runCatching {
        context.findActivity().enterPictureInPictureMode(params.build())
        true
    }.getOrDefault(false)
}
