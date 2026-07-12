package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy

/**
 * Returns all media buckets (folders) for the given [dataType].
 * Returns an empty list on platforms without a media store (iOS).
 */
expect suspend fun getMediaBuckets(dataType: DataType): List<DMediaBucket>

/**
 * Search media items of [dataType] matching [query], paginated by [limit]/[offset]
 * and sorted by [sortBy]. Returns an empty list on unsupported platforms.
 */
expect suspend fun searchMedia(
    dataType: DataType,
    query: String,
    limit: Int,
    offset: Int,
    sortBy: FileSortBy,
): List<IData>

/**
 * Count media items of [dataType] matching [query]. Returns 0 on unsupported platforms.
 */
expect suspend fun countMedia(dataType: DataType, query: String): Int

/**
 * Move media items of [dataType] identified by [ids] to the trash.
 */
expect suspend fun trashMedia(dataType: DataType, ids: Set<String>)

/**
 * Restore media items of [dataType] identified by [ids] from the trash.
 */
expect suspend fun restoreMedia(dataType: DataType, ids: Set<String>)
