package com.ismartcoding.plain.platform

import android.net.Uri
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.codeeditor.ByteSource
import com.ismartcoding.plain.lib.codeeditor.ChunkedByteSource
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual fun openByteSource(path: String): ByteSource {
    val channel: FileChannel = if (path.startsWith("content://")) {
        val pfd = appContext.contentResolver.openFileDescriptor(Uri.parse(path), "r")
            ?: throw IllegalArgumentException("cannot open $path")
        try {
            FileInputStream(pfd.fileDescriptor).channel
        } catch (e: Exception) {
            runCatching { pfd.close() }
            throw e
        }
    } else {
        val filePath = if (path.startsWith("file://")) Uri.parse(path).path else path
        FileInputStream(File(filePath ?: path)).channel
    }
    val source = object : ByteSource {
        override val size: Long = channel.size()

        @Synchronized
        override fun readAt(offset: Long, length: Int): ByteArray {
            val buffer = ByteBuffer.allocate(length)
            var read = 0
            while (read < length) {
                val n = channel.read(buffer, offset + read)
                if (n < 0) break
                read += n
            }
            if (read < length) throw IllegalStateException("short read at $offset")
            return buffer.array()
        }

        override fun close() {
            runCatching { channel.close() }
        }
    }
    return ChunkedByteSource(source)
}

actual fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>): Boolean {
    return try {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            appContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                while (chunks.hasNext()) {
                    out.write(chunks.next())
                }
            } ?: return false
            true
        } else {
            val filePath = if (path.startsWith("file://")) Uri.parse(path).path else path
            val target = File(filePath ?: path)
            val tmp = File(target.parentFile, target.name + ".ce_tmp")
            FileOutputStream(tmp).use { fos ->
                val out = fos.channel
                while (chunks.hasNext()) {
                    out.write(ByteBuffer.wrap(chunks.next()))
                }
                out.force(true)
            }
            if (target.exists()) {
                target.delete()
            }
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        }
    } catch (e: Exception) {
        false
    }
}
