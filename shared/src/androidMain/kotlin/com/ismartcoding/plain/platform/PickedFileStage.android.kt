package com.ismartcoding.plain.platform

import android.net.Uri
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.withIO
import java.io.File

actual suspend fun stagePickedFile(uri: String, path: String) = withIO {
    val target = File(path)
    target.parentFile?.mkdirs()
    val source = Uri.parse(uri)
    val input = if (source.scheme == "content") appContext.contentResolver.openInputStream(source)
        else File(source.path ?: uri).inputStream()
    checkNotNull(input) { "Unable to read selected URI" }.use { reader -> target.outputStream().use { writer -> reader.copyTo(writer) } }
    Unit
}
