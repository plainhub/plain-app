package com.ismartcoding.plain

import coil3.SingletonImageLoader
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.features.settings.Language
import com.ismartcoding.plain.platform.system.LauncherShortcuts
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.initDiskLogging
import com.ismartcoding.plain.platform.system.initializePlatformApp
import com.ismartcoding.plain.platform.system.newImageLoader
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.ui.settings.checkAppUpdate

fun initializeApp() {
    SingletonImageLoader.setSafe(::newImageLoader)
    RustRuntime.start()
    initDiskLogging()
    AppEvents.register()
    initializePlatformApp()
    Language.observeLocale()
    LauncherShortcuts.observeSelection()
    if (Capability.APP_UPDATES in getDeviceCapabilities()) {
        coIO { checkAppUpdate(showToast = false, automatic = true) }
    }
}
