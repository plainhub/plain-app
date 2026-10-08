package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ai.ImageIndexManager

internal actual suspend fun observeImageModels(enabled: Boolean) {
    if (enabled) ImageIndexManager.startup() else ImageIndexManager.shutdown()
}
