package com.ismartcoding.plain.platform

internal actual fun imageModelsAvailableOnPlatform() = false
internal actual suspend fun loadImageModels(files: ImageModelsFiles) { error("Image inference is unavailable") }
internal actual suspend fun closeImageModels() {}

internal actual suspend fun embedImageSearchText(tokenIds: List<Int>): FloatArray = error("Image inference is unavailable")
