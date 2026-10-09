package com.ismartcoding.plain

import com.ismartcoding.plain.preferences.*

import android.app.Application
import android.media.AudioAttributes
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import coil3.SingletonImageLoader
import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isUPlus
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.initDiskLogging
import com.ismartcoding.plain.preferences.setDarkMode
import com.ismartcoding.plain.preferences.ensureAdbToken
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.platform.newImageLoader
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator

object MainAppHelper {

    fun init(app: Application) {
        com.ismartcoding.plain.thumbnail.ThumbnailProvider.instance = com.ismartcoding.plain.thumbnail.ThumbnailGenerator
        Prefs.load()
        com.ismartcoding.plain.api.RustContentApi.start()
        CrashHandler.install(app)

        SingletonImageLoader.setSafe { context -> newImageLoader(context) }

        // Disk logging + HTTP request logging (debug=VERBOSE, release=WARN)
        initDiskLogging()

        AppEvents.register()
        NetworkMonitor.init(app)
        if (isQPlus()) {
            try {
                audioManager.allowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL
            } catch (_: Exception) {
            }
        }

        if (isUPlus()) {
            ZipPathValidator.clearCallback()
        }

        try {
            val manager = app.getSystemService(TextClassificationManager::class.java)
            manager?.setTextClassifier(TextClassifier.NO_OP)
        } catch (_: Throwable) {
        }

        coIO {
            initCommonPreferences()
            // Must run after initCommonPreferences: the keystore warm-up loads
            // keystore.bks with the stored password, which only exists once
            // SystemPrefs.ensureKeyStorePassword has run — warming up
            // earlier creates the file with an empty password and forces a
            // regenerate cycle on the first server start.
            SystemPrefs.setDarkMode(DarkTheme.parse(UserPrefs.darkTheme.value))
            SystemPrefs.ensureAdbToken()
            if (UserPrefs.service.value && PlugInControlReceiver.isUSBConnected(app)) {
                sendEvent(PowerConnectedEvent())
            }

            if (UserPrefs.feedAutoRefresh.value) {
                FeedFetchWorker.startRepeatWorkerAsync(app)
            }
            ImageSearchManager.restoreIfEnabled()

            val updateInfo = SystemPrefs.updateInfoValue()
            val checkUpdateTime = updateInfo.checkUpdateTime
            val autoCheckUpdate = updateInfo.autoCheckUpdate
            if (AppFeatureType.CHECK_UPDATES.has() && autoCheckUpdate && checkUpdateTime < System.currentTimeMillis() - Constants.ONE_DAY_MS) {
                AppHelper.checkUpdateAsync(app, false)
            }
        }
    }
}
