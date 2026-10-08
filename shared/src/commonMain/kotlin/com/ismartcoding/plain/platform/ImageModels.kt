package com.ismartcoding.plain.platform

internal expect suspend fun observeImageModels(enabled: Boolean)

suspend fun releaseImageModelMemory() {
    com.ismartcoding.plain.ai.RustImageModels.call(com.ismartcoding.plain.ai.ImageModelsCommand.Release)
}
