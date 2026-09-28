package com.ismartcoding.plain.platform

import java.io.File

actual fun ensureDir(path: String) {
    val dir = File(path)
    if (!dir.exists()) dir.mkdirs()
}

actual fun appendLine(path: String, line: String): Long {
    val file = File(path)
    file.appendText(line)
    return file.length()
}

actual fun deleteFileIfExists(path: String) {
    val file = File(path)
    if (file.exists()) file.delete()
}

actual fun renameFile(from: String, to: String) {
    File(from).renameTo(File(to))
}
