package com.ismartcoding.plain.web.loaders

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.plain.helpers.ImageHelper
import com.ismartcoding.plain.web.models.AudioFileInfo
import com.ismartcoding.plain.web.models.ImageFileInfo
import com.ismartcoding.plain.web.models.Location
import com.ismartcoding.plain.web.models.VideoFileInfo
import java.io.File

object FileInfoLoader {
    fun loadImage(
        path: String,
    ): ImageFileInfo {
        val rotation = ImageHelper.getRotation(path)
        val size = ImageHelper.getIntrinsicSize(path, rotation)
        var location: Location? = null
        if (!path.endsWith(".svg", true)) {
            val exifInterface = ExifInterface(path)
            val latLong = exifInterface.latLong
            if (latLong != null) {
                location = Location(latLong[0], latLong[1])
            }
        }
        return ImageFileInfo(size.width, size.height, location)
    }

    fun loadVideo(
        context: Context,
        path: String,
    ): VideoFileInfo {
        val file = File(path)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.div(1000) ?: 0L
        val location = parseLocationString(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
        retriever.release()
        return VideoFileInfo(width, height, duration, location)
    }

    fun loadAudio(
        context: Context,
        path: String,
    ): AudioFileInfo {
        val file = File(path)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.div(1000) ?: 0L
        val location = parseLocationString(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION))
        retriever.release()
        return AudioFileInfo(duration, location)
    }

    private fun parseLocationString(location: String?): Location? {
        try {
            if (location != null) {
                val regex = Regex("([+\\-]\\d{1,3}\\.\\d{4})([+\\-]\\d{1,3}\\.\\d{4})")
                val matchResult = regex.find(location)
                if (matchResult != null) {
                    val (latitudeStr, longitudeStr) = matchResult.destructured
                    val latitude = latitudeStr.toDouble()
                    val longitude = longitudeStr.toDouble()
                    return Location(latitude, longitude)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
