package com.ismartcoding.plain

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.ismartcoding.lib.brv.utils.BRV
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
import com.ismartcoding.plain.ui.helpers.PageHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import dalvik.system.ZipPathValidator
import kotlinx.coroutines.flow.first

class MainApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val memoryPercent = if (activityManager.isLowRamDevice) 0.25 else 0.75
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory(false))
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(percent = memoryPercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").absoluteFile)
                    .maxSizePercent(1.0)
                    .build()
            }
            .crossfade(100)
            // Ignore the network cache headers and always read from/write to the disk cache.
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        LogCat.addLogAdapter(DiskLogAdapter(DiskLogFormatStrategy.getInstance(this)))
        BRV.modelId = BR.m

        PageHelper.init()

        BluetoothEvents.register()
        AppEvents.register()
        // BoxEvents.register()

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
