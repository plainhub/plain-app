package com.ismartcoding.plain.platform

import android.os.Bundle
import android.provider.MediaStore
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.toSortBy
import com.ismartcoding.plain.lib.extensions.getLongValue
import com.ismartcoding.plain.lib.extensions.getStringValue
import com.ismartcoding.plain.lib.extensions.map
import com.ismartcoding.plain.lib.extensions.pathToUri
import com.ismartcoding.plain.lib.extensions.paging
import com.ismartcoding.plain.lib.extensions.sort
import com.ismartcoding.plain.lib.extensions.where
import com.ismartcoding.plain.lib.withIO

private val audioUri get() = if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
private fun requireAudioPermission() {
    check(Permission.WRITE_EXTERNAL_STORAGE.isGranted() || Permission.READ_MEDIA_AUDIO.isGranted()) { "Audio library permission denied" }
}
actual suspend fun audioLibraryCount(): Int = withIO {
    requireAudioPermission()
    checkNotNull(appContext.contentResolver.query(audioUri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)) { "Audio catalog query failed" }.use { it.count }
}
actual suspend fun audioLibraryContains(path: String): Boolean = withIO {
    requireAudioPermission()
    checkNotNull(appContext.contentResolver.query(audioUri, arrayOf(MediaStore.Audio.Media._ID), "${MediaStore.Audio.Media.DATA} = ?", arrayOf(path), null)) { "Audio catalog query failed" }.use { it.count > 0 }
}
actual suspend fun audioLibraryPage(offset: Int, limit: Int, sort: FileSortBy): List<DPlaylistAudio> = withIO {
    requireAudioPermission()
    require(offset >= 0 && limit >= 0)
    if (limit == 0) return@withIO emptyList()
    val columns = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION)
    val sortBy = sort.toSortBy()
    val cursor = if (isRPlus()) {
        appContext.contentResolver.query(audioUri, columns, Bundle().apply { paging(offset, limit); sort(sortBy); where("", emptyList()) }, null)
    } else {
        appContext.contentResolver.query(audioUri, columns, null, null, "${sortBy.field} ${sortBy.direction} LIMIT $limit OFFSET $offset")
    }
    checkNotNull(cursor) { "Audio catalog query failed" }.map { row, cache ->
        DPlaylistAudio(title = row.getStringValue(MediaStore.Audio.Media.TITLE, cache), path = row.getStringValue(MediaStore.Audio.Media.DATA, cache), artist = row.getStringValue(MediaStore.Audio.Media.ARTIST, cache).replace(MediaStore.UNKNOWN_STRING, ""), albumId = row.getStringValue(MediaStore.Audio.Media.ALBUM_ID, cache), durationMs = row.getLongValue(MediaStore.Audio.Media.DURATION, cache))
    }
}

actual suspend fun audioLibraryMetadata(path: String): DPlaylistAudio = withIO {
    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(appContext, path.pathToUri())
        DPlaylistAudio(
            title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().ifEmpty { java.io.File(path).nameWithoutExtension },
            path = path,
            artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
            durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0,
        )
    } finally { retriever.release() }
}
