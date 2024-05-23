package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.Window
import com.ismartcoding.plain.ui.MainActivity

internal fun Context.findActivity(): Activity {
    return MainActivity.instance.get()!!
}

internal fun Context.isActivityStatePipMode(): Boolean {
    return findActivity().isInPictureInPictureMode
}

internal fun Activity.setFullScreen(fullscreen: Boolean) {
    window.setFullScreen(fullscreen)
}

@Suppress("Deprecation")
internal fun Window.setFullScreen(fullscreen: Boolean) {
    if (fullscreen) {
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    } else {
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}
