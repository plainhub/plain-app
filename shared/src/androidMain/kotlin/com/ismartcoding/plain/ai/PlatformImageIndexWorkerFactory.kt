package com.ismartcoding.plain.ai

import java.io.File

object PlatformImageIndexWorkerFactory : ImageIndexWorkerFactory {
    override fun isReady(): Boolean = ImageSearchManager.isModelReady()
    override fun create(): ImageIndexWorker = ImageEmbedWorker(File(ImageSearchManager.getModelDir(),"mobileclip_s2_image.tflite"))
}
