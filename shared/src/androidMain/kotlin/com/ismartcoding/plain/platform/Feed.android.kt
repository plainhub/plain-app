package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import android.os.Environment
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import java.io.File

actual fun saveFeedImage(feedId: String, imageUrl: String, bytes: ByteArray): String? {
    val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path + "/feeds/${feedId}"
    File(dir).mkdirs()
    var path = "$dir/main-${sha1(imageUrl.encodeToByteArray())}"
    val extension = imageUrl.getFilenameExtension()
    if (extension.isNotEmpty()) {
        path += ".$extension"
    }
    File(path).writeBytes(bytes)
    return path
}
