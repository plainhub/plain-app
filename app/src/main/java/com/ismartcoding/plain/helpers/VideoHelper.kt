package com.ismartcoding.plain.helpers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import java.io.File

object VideoHelper {
    fun getIntrinsicSize(context: Context, path: String): IntSize {
        val file = File(path)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        retriever.release()
        return IntSize(width, height)
    }
}