package com.ismartcoding.lib.pdfviewer.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object Util {
    private const val DEFAULT_BUFFER_SIZE = 1024 * 4

    @Throws(IOException::class)
    fun toByteArray(inputStream: InputStream): ByteArray {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var n: Int
        while (inputStream.read(buffer).also { n = it } != -1) {
            os.write(buffer, 0, n)
        }
        return os.toByteArray()
    }
}
