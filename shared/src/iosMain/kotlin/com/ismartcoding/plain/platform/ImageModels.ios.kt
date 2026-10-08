package com.ismartcoding.plain.platform

internal actual suspend fun observeImageModels(enabled: Boolean) = IosImageIndexCatalog.observe(enabled)
