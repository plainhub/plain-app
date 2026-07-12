package com.ismartcoding.plain.lib.extensions

import android.net.Uri
import android.provider.MediaStore
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import com.ismartcoding.plain.platform.isQPlus
import java.io.File
import java.text.Normalizer

fun String.pathToUri(): Uri {
    if (startsWith("/")) {
        return Uri.fromFile(File(this))
    }

    return Uri.parse(this)
}

// remove diacritics, for example č -> c
fun String.normalizeString(): String =
    Normalizer.normalize(
        this,
        Normalizer.Form.NFD,
    ).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

fun String.normalizePhoneNumber(): String = PhoneNumberUtils.normalizeNumber(this)

// if we are comparing phone numbers, compare just the last 9 digits
fun String.trimToComparableNumber(): String {
    val normalizedNumber = this.normalizeString()
    val startIndex = Math.max(0, normalizedNumber.length - 9)
    return normalizedNumber.substring(startIndex)
}

fun String.isImageSlow() =
    isImageFast() || getMimeType().startsWith("image") || startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())

fun String.isVideoSlow() =
    isVideoFast() || getMimeType().startsWith("video") || startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())

fun String.isAudioSlow() =
    isAudioFast() || getMimeType().startsWith("audio") || startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())

fun String.pathToMediaStoreBaseUri(): Uri {
    return when {
        isImageFast() -> if (isQPlus()) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        isVideoFast() -> if (isQPlus()) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        isAudioFast() -> if (isQPlus()) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }
}

fun String.pathToMediaStoreUri(id: String): Uri {
    return Uri.withAppendedPath(pathToMediaStoreBaseUri(), id)
}

fun String.isEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isPhone(): Boolean {
    return Patterns.PHONE.matcher(this).matches()
}
