package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.ui.platform.system.observeWindowTheme

internal actual val supportsBackgroundService: Boolean = false

internal actual fun initializePlatformApp() {
    observeWindowTheme()
    IosPermissionEvents.register()
    IosFilePickerEvents.register()
    IosWindowFocusEvents.register()
}
