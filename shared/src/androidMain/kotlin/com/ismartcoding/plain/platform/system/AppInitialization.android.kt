package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.network.NetworkEvents
import com.ismartcoding.plain.app.AppHelper
import com.ismartcoding.plain.app.HttpServerForegroundServiceManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.preferences.setDarkMode
import com.ismartcoding.plain.features.PackageHelper
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.components.DialogHelper
import java.io.File
import kotlin.system.exitProcess

internal actual fun initializePlatformApp() {
    MainViewModel.relaunchApp = {
        try {
            AppHelper.relaunch(appContext)
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        }
    }
    MainViewModel.installAppUpdate = { path ->
        try {
            PackageHelper.install(appContext, File(path))
            exitProcess(0)
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        }
    }
    MainViewModel.addMediaShortcut = { path, label ->
        try {
            MediaShortcutHelper.addToDesktop(appContext, path, label, appResourceMipmap("ic_launcher"))
        } catch (error: Exception) {
            DialogHelper.showMessage(error)
        }
    }
    NetworkEvents.changed()
    setDarkMode(Prefs.darkTheme.value)
    HttpServerForegroundServiceManager.observe()
    HttpServerResources.observe()
}
