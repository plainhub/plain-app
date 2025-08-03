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
import com.ismartcoding.plain.events.AcquireWakeLockEvent
import com.ismartcoding.plain.events.AppEvents
import com.ismartcoding.plain.events.StartNearbyServiceEvent
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import com.ismartcoding.plain.preferences.CheckUpdateTimePreference
import com.ismartcoding.plain.preferences.ClientIdPreference
import com.ismartcoding.plain.preferences.DarkThemePreference
import com.ismartcoding.plain.preferences.FeedAutoRefreshPreference
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.HttpsPreference
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.preferences.SignatureKeyPreference
import com.ismartcoding.plain.preferences.UrlTokenPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.receivers.PlugInControlReceiver
import com.ismartcoding.plain.web.ChatApiManager
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this

        LogCat.addLogAdapter(DiskLogAdapter(DiskLogFormatStrategy.getInstance(this)))

        AppEvents.register()

        // https://stackoverflow.com/questions/77683434/the-getnextentry-method-of-zipinputstream-throws-a-zipexception-invalid-zip-ent
        if (isUPlus()) {
            ZipPathValidator.clearCallback()
        }

        coIO {
            val preferences = dataStore.getPreferencesAsync()
            TempData.webEnabled = WebPreference.get(preferences)
            TempData.webHttps = HttpsPreference.get(preferences)
            TempData.httpPort = HttpPortPreference.get(preferences)
            TempData.httpsPort = HttpsPortPreference.get(preferences)
            TempData.audioPlayMode = AudioPlayModePreference.getValue(preferences)
            val checkUpdateTime = CheckUpdateTimePreference.get(preferences)
            ClientIdPreference.ensureValueAsync(instance, preferences)
            KeyStorePasswordPreference.ensureValueAsync(instance, preferences)
            UrlTokenPreference.ensureValueAsync(instance, preferences)
            SignatureKeyPreference.ensureKeyPairAsync(instance, preferences)
            MdnsHostnamePreference.ensureValueAsync(preferences)

            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))
            if (PlugInControlReceiver.isUSBConnected(this@MainApp)) {
                sendEvent(AcquireWakeLockEvent())
            }
            if (PasswordPreference.get(preferences).isEmpty()) {
                HttpServerManager.resetPasswordAsync()
            }
            HttpServerManager.loadTokenCache()
            ChatApiManager.loadKeyCacheAsync()
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(instance)
            }
            // Start Nearby service (always listen regardless of discoverable setting)
            sendEvent(StartNearbyServiceEvent())
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
