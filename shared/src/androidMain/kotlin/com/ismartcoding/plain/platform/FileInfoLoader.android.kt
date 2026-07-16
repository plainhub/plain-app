package com.ismartcoding.plain.platform

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.web.models.AudioFileInfo
import com.ismartcoding.plain.web.models.ImageFileInfo
import com.ismartcoding.plain.web.models.VideoFileInfo
import com.ismartcoding.plain.web.models.parseMediaLocation
import android.net.Uri
import java.io.File

actual fun loadImageInfo(path: String): ImageFileInfo {
    val rotation = ImageHelper.getRotation(path)
    val size = ImageHelper.getIntrinsicSize(path, rotation)
    var location: com.ismartcoding.plain.web.models.Location? = null
    if (!path.endsWith(".svg", true)) {
        val exifInterface = ExifInterface(path)
        val latLong = exifInterface.latLong
        if (latLong != null) {
            location = com.ismartcoding.plain.web.models.Location(latLong[0], latLong[1])
        }
    }
    return ImageFileInfo(size.width, size.height, location)
}

actual fun loadVideoInfo(path: String): VideoFileInfo {
    val file = File(path)
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(appContext, Uri.fromFile(file))
    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.div(1000) ?: 0L
    val location = parseMediaLocation(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
    retriever.release()
    return VideoFileInfo(width, height, duration, location)
}

actual fun loadAudioInfo(path: String): AudioFileInfo {
    val file = File(path)
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(appContext, Uri.fromFile(file))
    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.div(1000) ?: 0L
    val location = parseMediaLocation(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
    retriever.release()
    return AudioFileInfo(duration, location)
}
