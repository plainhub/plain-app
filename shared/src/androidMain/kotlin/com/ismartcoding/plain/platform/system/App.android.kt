package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.ui.platform.media.VideoPlayerCacheManager

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.platform.system.AppLogHelper
import java.io.File
import android.content.Intent
import android.provider.Settings
import coil3.SingletonImageLoader
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.powerManager

internal actual fun shareLogArchive(path: String) = AppLogHelper.share(appContext, File(path))

internal actual suspend fun clearMediaCache(): Unit = withIO {
    SingletonImageLoader.get(appContext).diskCache?.clear()
    SingletonImageLoader.get(appContext).memoryCache?.clear()
    VideoPlayerCacheManager.getCache()?.let { cache -> cache.keys.toList().forEach(cache::removeResource) }
}

fun isIgnoringBatteryOptimizations(): Boolean =
    powerManager.isIgnoringBatteryOptimizations(appContext.packageName)

fun openBatteryOptimizationSettings() {
    val intent = Intent().apply {
        action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(intent)
}
