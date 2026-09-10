package com.ismartcoding.plain.features

import android.service.quicksettings.TileService

/**
 * Quick-settings tile that triggers an immediate clipboard capture, mirroring
 * KDE Connect's ClipboardTileService: launching the floating activity takes
 * window focus (the only reliable way to read the clipboard on Android 10+
 * without READ_LOGS), the capture broadcasts to connected desktops.
 */
class ClipboardTileService : TileService() {
    @Suppress("DEPRECATION")
    override fun onClick() {
        super.onClick()
        startActivityAndCollapse(ClipboardFloatingActivity.getIntentWithToast(this))
    }
}
