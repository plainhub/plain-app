package com.ismartcoding.plain.lib.codeeditor

/**
 * Sequential line index builder. Produces the line vector for a [LineVectorDocument] in one
 * background pass; a 100MB source scans in well under two seconds so the UI can show
 * progress and render as soon as the whole index is published.
 */
class LineIndexBuilder(private val source: ByteSource) {
    private val starts = IntList(4096)
    private val lens = IntList(4096)
    private val crlf = BitList(4096)
    private var offset = 0L
    private var lineStart = 0L
    private var pending = ByteArray(0)
    private var pendingPos = 0
    private var crlfCount = 0
    private var lfCount = 0
    private var sawAnyByte = false

    val progress: Float
        get() = if (source.size == 0L) 1f else (offset.toDouble() / source.size).toFloat()

    /** Scans the whole source. Call from a background dispatcher; reads [ByteSource] sequentially. */
    fun buildAll(): LineVectorDocument {
        while (true) {
            if (pendingPos >= pending.size) {
                if (offset >= source.size) break
                val read = minOf(READ_SIZE.toLong(), source.size - offset).toInt()
                pending = source.readAt(offset, read)
                offset += read
                pendingPos = 0
                if (pending.isEmpty()) break
                sawAnyByte = true
            }
            while (pendingPos < pending.size) {
                val b = pending[pendingPos]
                pendingPos++
                if (b == NEWLINE_BYTE) {
                    val newlinePos = offset - pending.size + pendingPos - 1
                    var contentEnd = newlinePos
                    var terminatorLen = 1
                    if (contentEnd > lineStart) {
                        if (readByteAt(contentEnd - 1) == CR_BYTE) {
                            contentEnd--
                            terminatorLen = 2
                            crlfCount++
                        } else {
                            lfCount++
                        }
                    } else {
                        lfCount++
                    }
                    starts.add(lineStart.toInt())
                    lens.add((contentEnd - lineStart).toInt())
                    crlf.add(terminatorLen == 2)
                    lineStart = offset - pending.size + pendingPos
                }
            }
        }
        if (source.size == 0L && !sawAnyByte) {
            starts.add(0)
            lens.add(0)
            crlf.add(false)
        } else if (lineStart < source.size) {
            var contentEnd = source.size
            if (contentEnd > lineStart + 1 && readByteAt(contentEnd - 1) == CR_BYTE) {
                // Lone trailing CR is treated as a broken terminator, not content.
                contentEnd--
            }
            starts.add(lineStart.toInt())
            lens.add((contentEnd - lineStart).toInt())
            crlf.add(false)
        }
        return LineVectorDocument(
            source = source,
            starts = starts,
            lens = lens,
            crlf = crlf,
            dominantCrlf = crlfCount > lfCount,
            lastLineHasTerminator = lineStart >= source.size && sawAnyByte && source.size > 0,
        )
    }

    private var window: ByteArray = ByteArray(0)
    private var windowStart = -1L

    private fun readByteAt(pos: Long): Byte {
        if (pos < 0) return 0
        if (windowStart >= 0 && pos >= windowStart && pos < windowStart + window.size) {
            return window[(pos - windowStart).toInt()]
        }
        val from = (pos / 64) * 64
        val len = minOf(128L, source.size - from).toInt()
        window = source.readAt(from, len)
        windowStart = from
        return window[(pos - from).toInt()]
    }

    companion object {
        private const val READ_SIZE = 512 * 1024
        private const val NEWLINE_BYTE: Byte = 0x0A
        private const val CR_BYTE: Byte = 0x0D
    }
}

enum class DetectedEncoding { UTF8, UTF16LE, UTF16BE, BINARY }

object EncodingProbe {
    /**
     * Sniffs the head of the source. UTF-8 BOM is kept as content (round-trips on save);
     * UTF-16 files are converted wholesale by the caller (with a size guard).
     */
    fun probe(source: ByteSource): DetectedEncoding {
        val headLen = minOf(source.size, 64L * 1024).toInt()
        if (headLen == 0) return DetectedEncoding.UTF8
        val head = source.readAt(0, headLen)
        if (head.size >= 2) {
            if (head[0] == 0xFF.toByte() && head[1] == 0xFE.toByte()) return DetectedEncoding.UTF16LE
            if (head[0] == 0xFE.toByte() && head[1] == 0xFF.toByte()) return DetectedEncoding.UTF16BE
        }
        // UTF-16 without BOM: NUL bytes dominate (one per code unit). Parity of the NULs
        // reveals byte order; balanced NUL parity means binary content instead.
        var zeros = 0
        var zerosEven = 0
        var zerosOdd = 0
        val scan = minOf(head.size, 8192)
        for (i in 0 until scan) {
            if (head[i] == 0.toByte()) {
                zeros++
                if (i % 2 == 0) zerosEven++ else zerosOdd++
            }
        }
        // Heuristic needs a meaningful sample; tiny inputs fall through to the binary check.
        if (scan >= 32 && zeros * 100 >= scan * 25) {
            val evenPct = zerosEven * 100 / scan
            val oddPct = zerosOdd * 100 / scan
            if (oddPct >= 20 && evenPct < 8) return DetectedEncoding.UTF16LE
            if (evenPct >= 20 && oddPct < 8) return DetectedEncoding.UTF16BE
            return DetectedEncoding.BINARY
        }
        if (looksBinary(head)) return DetectedEncoding.BINARY
        return if (validUtf8(head)) DetectedEncoding.UTF8 else DetectedEncoding.UTF16LE
    }

    private fun looksBinary(head: ByteArray): Boolean {
        val limit = minOf(head.size, 4096)
        for (i in 0 until limit) {
            if (head[i] == 0.toByte()) return true
        }
        return false
    }

    private fun validUtf8(bytes: ByteArray): Boolean {
        var i = 0
        var continuation = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            when {
                continuation > 0 -> {
                    if (b and 0xC0 != 0x80) return false
                    continuation--
                }
                b < 0x80 -> {}
                b and 0xE0 == 0xC0 -> continuation = 1
                b and 0xF0 == 0xE0 -> continuation = 2
                b and 0xF8 == 0xF0 -> continuation = 3
                else -> return false
            }
            i++
        }
        return true
    }
}
