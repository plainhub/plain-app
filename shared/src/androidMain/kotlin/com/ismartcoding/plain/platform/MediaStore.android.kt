package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioMediaStoreHelper
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.docs.DocMediaStoreHelper
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.features.file.FileSortBy

actual suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket> {
    return when (dataType) {
        DataType.IMAGE -> ImageMediaStoreHelper.getBucketsAsync(appContext)
        DataType.VIDEO -> VideoMediaStoreHelper.getBucketsAsync(appContext)
        DataType.AUDIO -> if (isQPlus()) AudioMediaStoreHelper.getBucketsAsync(appContext) else emptyList()
        DataType.DOC -> DocMediaStoreHelper.getDocBucketsAsync(appContext)
        else -> emptyList()
    }
}

actual suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData> {
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.DOC -> DocMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.IMAGE -> ImageMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        DataType.VIDEO -> VideoMediaStoreHelper.searchAsync(appContext, query, limit, offset, sortBy)
        else -> emptyList()
    }
}

actual suspend fun countMedia(dataType: DataType, query: String): Int {
    return when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.countAsync(appContext, query)
        DataType.DOC -> DocMediaStoreHelper.countAsync(appContext, query)
        DataType.IMAGE -> ImageMediaStoreHelper.countAsync(appContext, query)
        DataType.VIDEO -> VideoMediaStoreHelper.countAsync(appContext, query)
        else -> 0
    }
}

actual suspend fun trashMedia(dataType: DataType, ids: Set<String>) {
    when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.DOC -> DocMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.IMAGE -> ImageMediaStoreHelper.trashByIdsAsync(appContext, ids)
        DataType.VIDEO -> VideoMediaStoreHelper.trashByIdsAsync(appContext, ids)
        else -> {}
    }
}

actual suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {
    when (dataType) {
        DataType.AUDIO -> AudioMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.DOC -> DocMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.IMAGE -> ImageMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        DataType.VIDEO -> VideoMediaStoreHelper.restoreByIdsAsync(appContext, ids)
        else -> {}
    }
}
