package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy

actual suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket> = emptyList()

actual suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData> = emptyList()

actual suspend fun countMedia(dataType: DataType, query: String): Int = 0

actual suspend fun trashMedia(dataType: DataType, ids: Set<String>) {}

actual suspend fun restoreMedia(dataType: DataType, ids: Set<String>) {}
