package com.ismartcoding.plain.platform

internal expect fun imageModelsAvailableOnPlatform(): Boolean
internal expect suspend fun loadImageModels(files: ImageModelsFiles)
internal expect suspend fun closeImageModels()

internal expect suspend fun embedImageSearchText(tokenIds: List<Int>): FloatArray
