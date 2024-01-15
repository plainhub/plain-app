package com.ismartcoding.lib.apk.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object Inputs {
    @Throws(IOException::class)
    fun readAll(`in`: InputStream): ByteArray {
        val buf = ByteArray(1024 * 8)
        ByteArrayOutputStream().use { bos ->
            var len: Int
            while (`in`.read(buf).also { len = it } != -1) {
                bos.write(buf, 0, len)
            }
            return bos.toByteArray()
        }
    }

    @Throws(IOException::class)
    fun readAllAndClose(`in`: InputStream): ByteArray {
        return try {
            readAll(`in`)
        } finally {
            `in`.close()
        }
    }
}