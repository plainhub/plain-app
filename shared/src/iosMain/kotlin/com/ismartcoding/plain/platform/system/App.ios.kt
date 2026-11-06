@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.system

import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.ismartcoding.plain.lib.withIO
import kotlinx.cinterop.ExperimentalForeignApi


internal actual fun shareLogArchive(path: String) = shareFile(path)

internal actual suspend fun clearMediaCache(): Unit = withIO {
    val loader = SingletonImageLoader.get(PlatformContext.INSTANCE)
    loader.diskCache?.clear()
    loader.memoryCache?.clear()
}
