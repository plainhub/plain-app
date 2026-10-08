package com.ismartcoding.plain.ai

import java.io.File

object PlatformImageIndexWorkerFactory : ImageIndexWorkerFactory {
    @Volatile private var modelFile: File? = null
    internal fun configure(path: String?) { modelFile = path?.let(::File) }
    override fun isReady(): Boolean = modelFile != null
    override fun create(): ImageIndexWorker = ImageEmbedWorker(checkNotNull(modelFile))
}
