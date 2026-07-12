package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.intPreferencesKey
import com.ismartcoding.plain.features.file.FileSortBy

abstract class BaseSortByPreference(
    val prefix: String,
    private val defaultSort: FileSortBy = FileSortBy.DATE_DESC
) : BasePreference<Int>() {
    override val default = defaultSort.ordinal
    override val key = intPreferencesKey("${prefix}_sort_by")

    suspend fun putAsync(value: FileSortBy) {
        putAsync(value.ordinal)
    }

    suspend fun getValueAsync(): FileSortBy {
        val value = getAsync()
        return FileSortBy.entries.find { it.ordinal == value } ?: defaultSort
    }
}

object AudioSortByPreference : BaseSortByPreference("audio", FileSortBy.DATE_DESC)
object VideoSortByPreference : BaseSortByPreference("video", FileSortBy.TAKEN_AT_DESC)
object ImageSortByPreference : BaseSortByPreference("image", FileSortBy.TAKEN_AT_DESC)
object DocSortByPreference : BaseSortByPreference("doc")
object FileSortByPreference : BaseSortByPreference("file", FileSortBy.NAME_ASC)
object PackageSortByPreference : BaseSortByPreference("pkg", FileSortBy.NAME_ASC)
