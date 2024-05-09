package com.ismartcoding.plain.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import androidx.compose.ui.unit.IntSize

object MediaHelper {
    fun getImageIntrinsicSize(path: String): IntSize {
        if (path.endsWith(".svg", true)) {
            return SvgHelper.getSize(path)
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return IntSize(options.outWidth, options.outHeight)
    }

    fun getVideoIntrinsicSize(context: Context, path: String): IntSize {
        val file = File(path)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        retriever.release()
        return IntSize(width, height)
    }
}