package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DataType

sealed interface MediaSortBy {
    val name: String
    val dataType: DataType
    val graphqlType: String
        get() = when (dataType) {
            DataType.IMAGE -> "ImageSortBy"
            DataType.VIDEO -> "VideoSortBy"
            DataType.DOC -> "DocSortBy"
            DataType.AUDIO -> "AudioSortBy"
            else -> error("Unsupported media type: $dataType")
        }
}

enum class ImageSortBy : MediaSortBy {
    UPDATED_AT_ASC,
    UPDATED_AT_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    TAKEN_AT_DESC,
    ;
    override val dataType: DataType get() = DataType.IMAGE
}

enum class VideoSortBy : MediaSortBy {
    UPDATED_AT_ASC,
    UPDATED_AT_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    TAKEN_AT_DESC,
    ;
    override val dataType: DataType get() = DataType.VIDEO
}

enum class DocSortBy : MediaSortBy {
    UPDATED_AT_ASC,
    UPDATED_AT_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC,
    ;
    override val dataType: DataType get() = DataType.DOC
}

enum class AudioSortBy : MediaSortBy {
    UPDATED_AT_ASC,
    UPDATED_AT_DESC,
    SIZE_ASC,
    SIZE_DESC,
    TITLE_ASC,
    TITLE_DESC,
    ;
    override val dataType: DataType get() = DataType.AUDIO
}

fun mediaSortEntries(type: DataType): List<MediaSortBy> = when (type) {
    DataType.IMAGE -> ImageSortBy.entries
    DataType.VIDEO -> VideoSortBy.entries
    DataType.DOC -> DocSortBy.entries
    DataType.AUDIO -> AudioSortBy.entries
    else -> error("Unsupported media type: $type")
}

fun mediaSortBy(type: DataType, name: String): MediaSortBy =
    mediaSortEntries(type).first { it.name == name }
