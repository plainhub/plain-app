package com.ismartcoding.plain.platform

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode

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

actual fun setImmersiveFullscreen() {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    val view = activity.window.decorView
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    WindowInsetsControllerCompat(activity.window, view).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

actual fun exitImmersiveFullscreen() {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    val view = activity.window.decorView
    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
    WindowInsetsControllerCompat(activity.window, view).apply {
        show(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }
}

@Composable
actual fun rememberWindowInsetsController(): Any {
    val window = with(LocalActivity.current as Activity) { return@with window }
    return remember { WindowCompat.getInsetsController(window, window.decorView) }
}

actual fun applySystemBarAppearanceForDarkTheme(useDarkTheme: Boolean) {
    val context = com.ismartcoding.plain.appContextValue ?: return
    val activity = context as? Activity ?: return
    val window = activity.window
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !useDarkTheme
        isAppearanceLightNavigationBars = !useDarkTheme
    }
    if (isQPlus() && isGestureInteractionMode()) {
        window.isNavigationBarContrastEnforced = false
    }
}
