package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ai.*
import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.enums.AppChannelType
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

private val modelHandles = Mutex()
internal actual fun imageModelsAvailableOnPlatform() = buildChannel != AppChannelType.FDROID.name
internal actual suspend fun loadImageModels(files: ImageModelsFiles) = modelHandles.withLock {
    withIO {
        try {
            ImageEmbedHelper.init(File(files.imageModel))
            TextEmbedHelper.init(File(files.textModel))
            PlatformImageIndexWorkerFactory.configure(files.imageModel)
            ImageIndexManager.startup()
        } catch (error: Throwable) {
            closeHandles()
            throw error
        }
    }
}
internal actual suspend fun closeImageModels() = modelHandles.withLock { withIO { closeHandles() } }
private suspend fun closeHandles() {
    ImageIndexManager.shutdown()
    PlatformImageIndexWorkerFactory.configure(null)
    ImageEmbedHelper.close()
    TextEmbedHelper.close()
    DelegateHelper.closeAll()
}

internal actual suspend fun embedImageSearchText(tokenIds: List<Int>): FloatArray = modelHandles.withLock {
    withIO {
        require(tokenIds.size == 77)
        TextEmbedHelper.embed(tokenIds.toIntArray()) ?: error("Image search query inference failed")
    }
}

suspend fun releaseImageModelMemory() = modelHandles.withLock {
    withIO {
        ImageEmbedHelper.release()
        TextEmbedHelper.release()
    }
}
