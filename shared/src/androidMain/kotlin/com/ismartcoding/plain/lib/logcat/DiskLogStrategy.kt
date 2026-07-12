package com.ismartcoding.plain.lib.logcat

import java.io.File

internal actual fun ensureLogDir(path: String) {
    val dir = File(path)
    if (!dir.exists()) dir.mkdirs()
}

internal actual fun appendLogLine(path: String, line: String): Long {
    val file = File(path)
    file.appendText(line)
    return file.length()
}

internal actual fun deleteFileIfExists(path: String) {
    val file = File(path)
    if (file.exists()) file.delete()
}

internal actual fun renameFile(from: String, to: String) {
    File(from).renameTo(File(to))
}
