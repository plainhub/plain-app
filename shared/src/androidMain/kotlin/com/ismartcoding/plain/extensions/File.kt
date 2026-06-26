package com.ismartcoding.plain.extensions

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

fun File.getDirectChildrenCount(countHiddenItems: Boolean): Int {
    if (countHiddenItems) {
        return list()?.size ?: 0
    }
    return list()?.filter {
        !it.startsWith('.')
    }?.size ?: 0
}

fun File.newName(): String {
    var index = 1
    var candidate: String
    val split = nameWithoutExtension.split(' ').toMutableList()
    val last = split.last()
    if ("""^\(\d+\)$""".toRegex().matches(last)) {
        split.remove(last)
    }
    val name = split.joinToString(" ")
    while (true) {
        candidate = if (extension.isEmpty()) "$name ($index)" else "$name ($index).$extension"
        if (!File("$parent/$candidate").exists()) {
            return candidate
        }
        index++
    }
}

fun File.newPath(): String {
    return "$parent/" + newName()
}

fun File.newFile(): File {
    return File(newPath())
}

fun File.getDuration(context: Context): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.fromFile(this))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        (time?.toLong()?.div(1000)) ?: 0L
    } catch (ex: Exception) {
        0L
    } finally {
        runCatching { retriever.release() }
    }
}
