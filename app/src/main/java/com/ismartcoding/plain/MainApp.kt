package com.ismartcoding.plain

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.ismartcoding.lib.brv.utils.BRV
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.DiskLogAdapter
import com.ismartcoding.lib.logcat.DiskLogFormatStrategy
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.enums.DarkTheme
import com.ismartcoding.plain.data.enums.PasswordType
import com.ismartcoding.plain.data.preference.ClientIdPreference
import com.ismartcoding.plain.data.preference.DarkThemePreference
import com.ismartcoding.plain.data.preference.FeedAutoRefreshPreference
import com.ismartcoding.plain.data.preference.HttpPortPreference
import com.ismartcoding.plain.data.preference.HttpsPortPreference
import com.ismartcoding.plain.data.preference.KeyStorePasswordPreference
import com.ismartcoding.plain.data.preference.PasswordTypePreference
import com.ismartcoding.plain.data.preference.WebPreference
import com.ismartcoding.plain.data.preference.dataStore
import com.ismartcoding.plain.features.AppEvents
import com.ismartcoding.plain.features.bluetooth.BluetoothEvents
import com.ismartcoding.plain.features.box.BoxEvents
import com.ismartcoding.plain.ui.helpers.PageHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.workers.FeedFetchWorker
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.flow.first
import okhttp3.Dispatcher
import okhttp3.OkHttpClient


class MainApp : Application(), ImageLoaderFactory {
    var httpServer: NettyApplicationEngine? = null

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    // Set the max size to 25% of the app's available memory.
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(filesDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .okHttpClient {
                // Don't limit concurrent network requests by host.
                val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .build()
            }
            // Show a short crossfade when loading images asynchronously.
            .crossfade(true)
            // Ignore the network cache headers and always read from/write to the disk cache.
            .respectCacheHeaders(false)
            // Enable logging to the standard Android log if this is a debug build.
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
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
        BoxEvents.register()

        coIO {
            val preferences = dataStore.data.first()
            TempData.webEnabled = WebPreference.get(preferences)
            TempData.httpPort = HttpPortPreference.get(preferences)
            TempData.httpsPort = HttpsPortPreference.get(preferences)
            ClientIdPreference.ensureValueAsync(instance, preferences)
            KeyStorePasswordPreference.ensureValueAsync(instance, preferences)

            DarkThemePreference.setDarkMode(DarkTheme.parse(DarkThemePreference.get(preferences)))

            if (PasswordTypePreference.getValue(preferences) == PasswordType.RANDOM) {
                HttpServerManager.resetPasswordAsync()
            }
            HttpServerManager.loadTokenCache()
            if (FeedAutoRefreshPreference.get(preferences)) {
                FeedFetchWorker.startRepeatWorkerAsync(instance)
            }
            HttpServerManager.clientTsInterval()
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
