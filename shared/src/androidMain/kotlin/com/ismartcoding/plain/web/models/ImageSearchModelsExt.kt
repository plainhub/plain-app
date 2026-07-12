package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.ai.ImageSearchManager
import com.ismartcoding.plain.ai.ImageSearchIndexer

fun buildImageSearchStatus(): ImageSearchStatus {
    val mgr = ImageSearchManager
    val indexer = ImageSearchIndexer
    return ImageSearchStatus(
        status = mgr.status.value.name,
        downloadProgress = mgr.downloadProgress.value,
        errorMessage = mgr.errorMessage.value,
        modelSize = mgr.totalModelSize(),
        modelDir = mgr.getModelDir(),
        isIndexing = indexer.isRunning,
        totalImages = indexer.totalImages,
        indexedImages = indexer.indexedImages,
    )
}
