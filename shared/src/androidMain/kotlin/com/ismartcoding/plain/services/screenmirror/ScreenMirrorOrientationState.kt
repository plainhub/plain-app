package com.ismartcoding.plain.services.screenmirror

import android.content.res.Configuration

internal class ScreenMirrorOrientationState(
    initialIsPortrait: Boolean,
    private val onOrientationChanged: (Boolean) -> Unit,
) {
    @Volatile
    var isPortrait = initialIsPortrait
        private set

    fun onConfigurationChanged(orientation: Int) {
        val newIsPortrait = when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> true
            Configuration.ORIENTATION_LANDSCAPE -> false
            else -> return
        }
        if (newIsPortrait == isPortrait) return

        isPortrait = newIsPortrait
        onOrientationChanged(newIsPortrait)
    }
}
