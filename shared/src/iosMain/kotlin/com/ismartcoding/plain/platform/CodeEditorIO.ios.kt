package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.codeeditor.ByteSource
import com.ismartcoding.plain.lib.codeeditor.ChunkedByteSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toLong
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.posix.O_CREAT
import platform.posix.O_RDONLY
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.close
import platform.posix.fstat
import platform.posix.fsync
import platform.posix.open
import platform.posix.pread
import platform.posix.stat
import platform.posix.write

@OptIn(ExperimentalForeignApi::class)
actual fun openByteSource(path: String): ByteSource {
    val fd = open(path, O_RDONLY)
    if (fd < 0) throw IllegalArgumentException("cannot open $path")
    val fileSize = memScoped {
        val st = alloc<stat>()
        if (fstat(fd, st.ptr) != 0) {
            close(fd)
            throw IllegalArgumentException("cannot stat $path")
        }
        st.st_size
    }
    val source = object : SynchronizedObject(), ByteSource {
        override val size: Long = fileSize

        override fun readAt(offset: Long, length: Int): ByteArray = synchronized(this) {
            val buffer = ByteArray(length)
            if (length > 0) {
                buffer.usePinned { pinned ->
                    var done = 0
                    while (done < length) {
                        val n = pread(fd, pinned.addressOf(done), (length - done).toULong(), offset + done)
                        if (n <= 0) throw IllegalStateException("short read at $offset")
                        done += n.toInt()
                    }
                }
            }
            buffer
        }

        override fun close() {
            close(fd)
        }
    }
    return ChunkedByteSource(source)
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>): Boolean {
    return try {
        val manager = NSFileManager.defaultManager
        val tmp = path + ".ce_tmp"
        manager.createFileAtPath(tmp, contents = null, attributes = null)
        val fd = open(tmp, O_WRONLY or O_CREAT or O_TRUNC, 0x1A4)
        if (fd < 0) return false
        try {
            while (chunks.hasNext()) {
                val bytes = chunks.next()
                bytes.usePinned { pinned ->
                    var done = 0
                    while (done < bytes.size) {
                        val n = write(fd, pinned.addressOf(done), (bytes.size - done).toULong())
                        if (n <= 0L) return false
                        done += n.toInt()
                    }
                }
            }
            fsync(fd)
        } finally {
            close(fd)
        }
        if (manager.fileExistsAtPath(path)) {
            manager.removeItemAtPath(path, error = null)
        }
        manager.moveItemAtPath(tmp, toPath = path, error = null)
        true
    } catch (e: Exception) {
        false
    }
}
