package com.ismartcoding.plain

import android.app.Application
import android.os.Build
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.isUPlus
import com.ismartcoding.lib.logcat.DiskLogAdapter
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.features.AcquireWakeLockEvent
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.features.bluetooth.BluetoothEvents
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preference.AudioPlayModePreference
import com.ismartcoding.plain.preference.CheckUpdateTimePreference
import com.ismartcoding.plain.preference.ClientIdPreference
import com.ismartcoding.plain.preference.DarkThemePreference
import com.ismartcoding.plain.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.preference.HttpPortPreference
import com.ismartcoding.plain.preference.HttpsPortPreference
import com.ismartcoding.plain.preference.HttpsPreference
import com.ismartcoding.plain.preference.KeyStorePasswordPreference
import com.ismartcoding.plain.preference.PasswordPreference
import com.ismartcoding.plain.preference.UrlTokenPreference
import com.ismartcoding.plain.preference.WebPreference
import com.ismartcoding.plain.preference.dataStore
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator
import kotlinx.coroutines.flow.first

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this

        LogCat.addLogAdapter(DiskLogAdapter(DiskLogFormatStrategy.getInstance(this)))

        BluetoothEvents.register()
        AppEvents.register()

        // https://stackoverflow.com/questions/77683434/the-getnextentry-method-of-zipinputstream-throws-a-zipexception-invalid-zip-ent
        if (isUPlus()) {
            ZipPathValidator.clearCallback()
        }

        coIO {
            val preferences = dataStore.data.first()
            TempData.webEnabled = WebPreference.get(preferences)
            TempData.webHttps = HttpsPreference.get(preferences)
            TempData.httpPort = HttpPortPreference.get(preferences)
            TempData.httpsPort = HttpsPortPreference.get(preferences)
            TempData.audioPlayMode = AudioPlayModePreference.getValue(preferences)
            val checkUpdateTime = CheckUpdateTimePreference.get(preferences)
            ClientIdPreference.ensureValueAsync(instance, preferences)
            KeyStorePasswordPreference.ensureValueAsync(instance, preferences)
            UrlTokenPreference.ensureValueAsync(instance, preferences)

            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))
            if (PlugInControlReceiver.isUSBConnected(this@MainApp)) {
                sendEvent(AcquireWakeLockEvent())
            }
            if (PasswordPreference.get(preferences).isEmpty()) {
                HttpServerManager.resetPasswordAsync()
            }
            HttpServerManager.loadTokenCache()
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(instance)
            }
            HttpServerManager.clientTsInterval()
            if (AppFeatureType.CHECK_UPDATES.has() && checkUpdateTime < System.currentTimeMillis() - Constants.ONE_DAY_MS) {
                AppHelper.checkUpdateAsync(this@MainApp, false)
            }
        }
    }

    companion object {
        lateinit var instance: MainApp

        fun getAppVersion(): String {
            return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        }

        fun getAndroidVersion(): String {
            return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")"
        }
    }
}
