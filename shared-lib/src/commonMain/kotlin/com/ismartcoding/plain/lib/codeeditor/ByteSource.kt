package com.ismartcoding.plain.lib.codeeditor

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Random-access byte reader over an immutable backing store (file, content uri, memory).
 * Implementations must be safe for positioned reads from any thread; callers confine
 * heavy scans to a background dispatcher.
 */
interface ByteSource : AutoCloseable {
    val size: Long

    /** Reads exactly [length] bytes at [offset]. Offset + length must not exceed [size]. */
    fun readAt(offset: Long, length: Int): ByteArray
}

class ByteArrayByteSource(private val bytes: ByteArray) : ByteSource {
    override val size: Long = bytes.size.toLong()

    override fun readAt(offset: Long, length: Int): ByteArray {
        require(offset >= 0 && length >= 0 && offset + length <= bytes.size) { "range $offset+$length out of bounds ${bytes.size}" }
        return bytes.copyOfRange(offset.toInt(), (offset + length).toInt())
    }

    override fun close() {}
}

/** Size-capped map with access-order eviction; works identically on JVM and Native. */
class LruMap<K, V>(private val cap: Int) {
    private val map = LinkedHashMap<K, V>()

    fun get(key: K): V? {
        val value = map.remove(key) ?: return null
        map[key] = value
        return value
    }

    fun put(key: K, value: V) {
        if (map.containsKey(key)) map.remove(key)
        map[key] = value
        while (map.size > cap) {
            map.remove(map.keys.first())
        }
    }

    fun remove(key: K) {
        map.remove(key)
    }

    fun clear() = map.clear()

    fun entriesSnapshot(): List<Pair<K, V>> = map.entries.map { it.key to it.value }
}

/**
 * LRU chunk cache decorator. Serving a line read from a 100MB file without caching would
 * hit the platform file API per line while scrolling; chunks amortize that to once per 256KB.
 */
class ChunkedByteSource(
    private val inner: ByteSource,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val maxChunks: Int = DEFAULT_MAX_CHUNKS,
) : SynchronizedObject(), ByteSource {
    private val chunks = LruMap<Long, ByteArray>(maxChunks)

    override val size: Long get() = inner.size

    override fun readAt(offset: Long, length: Int): ByteArray {
        if (length == 0) return ByteArray(0)
        if (length >= chunkSize) return inner.readAt(offset, length)
        val chunkIndex = (offset / chunkSize)
        val inChunk = (offset % chunkSize).toInt()
        if (inChunk + length <= chunkSize) {
            val chunk = chunkAt(chunkIndex)
            return chunk.copyOfRange(inChunk, inChunk + length)
        }
        // Spans two chunks: read both spans and stitch.
        val firstLen = chunkSize - inChunk
        val first = chunkAt(chunkIndex).copyOfRange(inChunk, chunkSize)
        val second = chunkAt(chunkIndex + 1).copyOfRange(0, length - firstLen)
        return first + second
    }

    private fun chunkAt(index: Long): ByteArray = synchronized(this) {
        chunks.get(index) ?: run {
            val from = index * chunkSize
            val len = minOf(chunkSize.toLong(), inner.size - from).toInt()
            val chunk = inner.readAt(from, len)
            chunks.put(index, chunk)
            chunk
        }
    }

    override fun close() {
        inner.close()
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 256 * 1024
        const val DEFAULT_MAX_CHUNKS = 64 // ~16MB resident cap
    }
}
