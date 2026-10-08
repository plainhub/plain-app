package com.ismartcoding.plain.platform

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.features.system.AudioInfoFacts
import com.ismartcoding.plain.features.system.ImageInfoFacts
import com.ismartcoding.plain.features.system.VideoInfoFacts
import android.net.Uri
import java.io.File

actual fun loadImageInfo(path: String): ImageInfoFacts {
    val rotation = ImageHelper.getRotation(path)
    val size = ImageHelper.getIntrinsicSize(path, rotation)
    var location: com.ismartcoding.plain.features.system.LocationFacts? = null
    if (!path.endsWith(".svg", true)) {
        val exifInterface = ExifInterface(path)
        val latLong = exifInterface.latLong
        if (latLong != null) {
            location = com.ismartcoding.plain.features.system.LocationFacts(latLong[0], latLong[1])
        }
    }
    return ImageInfoFacts(size.width, size.height, location)
}

actual fun loadVideoInfo(path: String): VideoInfoFacts {
    val file = File(path)
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(appContext, Uri.fromFile(file))
    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
    retriever.release()
    return VideoInfoFacts(width, height, durationMs, location)
}

actual fun loadAudioInfo(path: String): AudioInfoFacts {
    val file = File(path)
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(appContext, Uri.fromFile(file))
    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
    val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
    retriever.release()
    return AudioInfoFacts(durationMs, location)
}
