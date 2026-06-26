package com.ismartcoding.plain.lib.extensions

import android.net.Uri
import android.provider.MediaStore
import com.ismartcoding.plain.lib.Constants
import com.ismartcoding.plain.lib.isQPlus

private val typesMap =
    HashMap<String, String>().apply {
        put("323", "text/h323")
        put("3g2", "video/3gpp2")
        put("3gp", "video/3gpp")
        put("3gpp", "video/3gpp")
        put("aac", "audio/aac")
        put("avif", "image/avif")
        put("bmp", "image/bmp")
        put("flac", "audio/flac")
        put("gif", "image/gif")
        put("heic", "image/heic")
        put("heif", "image/heif")
        put("jpg", "image/jpeg")
        put("jpeg", "image/jpeg")
        put("m4a", "audio/mp4")
        put("m4v", "video/mp4")
        put("mkv", "video/x-matroska")
        put("mov", "video/quicktime")
        put("mp3", "audio/mpeg")
        put("mp4", "video/mp4")
        put("mpeg", "video/mpeg")
        put("ogg", "audio/ogg")
        put("opus", "audio/ogg")
        put("pdf", "application/pdf")
        put("png", "image/png")
        put("svg", "image/svg+xml")
        put("tif", "image/tiff")
        put("tiff", "image/tiff")
        put("wav", "audio/wav")
        put("webm", "video/webm")
        put("webp", "image/webp")
        put("wma", "audio/x-ms-wma")
    }

fun String.getMimeType(): String {
    return typesMap[getFilenameExtension().lowercase()] ?: ""
}

fun String.pathToMediaStoreBaseUri(): Uri {
    return when {
        isImageFast() -> if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        isVideoFast() -> if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        isAudioFast() -> if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
}

fun String.isImageFast() = Constants.PHOTO_EXTENSIONS.any { endsWith(it, true) }

fun String.isVideoFast() = Constants.VIDEO_EXTENSIONS.any { endsWith(it, true) }

fun String.isAudioFast() = Constants.AUDIO_EXTENSIONS.any { endsWith(it, true) }
