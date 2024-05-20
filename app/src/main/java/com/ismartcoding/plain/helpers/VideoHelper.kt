package com.ismartcoding.plain.helpers

import android.media.MediaMetadataRetriever
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DVideoMeta
import java.io.File


object VideoHelper {
    fun getMeta(path: String): DVideoMeta? {
        val file = File(path)
        if (!file.exists()) {
            return null
        }

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 0f
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: ""
            val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) ?: ""
            val writer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER) ?: ""
            val composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER) ?: ""

            retriever.release()

            DVideoMeta(width, height, rotation, duration, bitrate, frameRate, title, artist, album, genre, date, writer, composer)
        } catch (e: Exception) {
            LogCat.e(e.toString())
            null
        }
    }

    fun getIntrinsicSize(path: String): IntSize {
        val meta = getMeta(path) ?: return IntSize(0, 0)
        val w = meta.width
        val h = meta.height
        val rotation = meta.rotation
        return if (rotation == 90 || rotation == 270) {
            IntSize(h, w)
        } else {
            IntSize(w, h)
        }
    }
}