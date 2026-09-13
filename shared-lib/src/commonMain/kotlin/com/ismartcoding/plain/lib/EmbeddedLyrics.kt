package com.ismartcoding.plain.lib

/**
 * Extracts lyrics embedded in audio file metadata, reading the source strictly
 * forward. Supported containers: ID3v2 USLT (mp3), Vorbis comments
 * LYRICS/UNSYNCEDLYRICS (flac) and the MP4 ©lyr atom (m4a). Anything else
 * yields an empty string.
 */
object EmbeddedLyrics {
    abstract class Reader {
        /** Reads up to [length] bytes into [buffer] at [offset]; returns the count or -1 at EOF. */
        abstract fun read(buffer: ByteArray, offset: Int, length: Int): Int

        abstract fun close()

        fun readFully(length: Int): ByteArray {
            val out = ByteArray(length)
            var filled = 0
            while (filled < length) {
                val n = read(out, filled, length - filled)
                if (n <= 0) break
                filled += n
            }
            return if (filled == length) out else out.copyOf(filled)
        }

        fun skipFully(length: Long) {
            var remaining = length
            val scratch = ByteArray(8192)
            while (remaining > 0) {
                val n = read(scratch, 0, minOf(remaining, scratch.size.toLong()).toInt())
                if (n <= 0) return
                remaining -= n
            }
        }
    }

    private const val MAX_TAG_BYTES = 8 * 1024 * 1024
    private const val MAX_MOOV_BYTES = 16 * 1024 * 1024

    fun extract(reader: Reader): String {
        val head = reader.readFully(8)
        return when {
            head.size >= 3 && head.match(0, "ID3") -> extractId3(reader, head)
            head.size >= 8 && head.match(4, "ftyp") -> extractMp4(reader, head)
            head.size >= 8 && head.match(0, "fLaC") -> extractFlac(reader, head)
            else -> ""
        }
    }

    private fun extractId3(reader: Reader, head: ByteArray): String {
        val header = if (head.size >= 10) head else head + reader.readFully(10 - head.size)
        if (header.size < 10) return ""
        val version = header[3].toInt() and 0xFF
        val flags = header[5].toInt() and 0xFF
        val tagSize = minOf(syncsafe(header, 6), MAX_TAG_BYTES)
        val data = reader.readFully(tagSize).let { if (flags and 0x80 != 0) removeUnsync(it) else it }

        var pos = 0
        if (flags and 0x40 != 0 && data.size >= 4) {
            // Extended header: v2.4 sizes include themselves, v2.3 do not.
            pos += if (version >= 4) syncsafe(data, 0) else be32(data, 0) + 4
        }
        val idSize = if (version >= 3) 4 else 3
        val headerSize = idSize + if (version >= 3) 6 else 3
        while (pos + headerSize <= data.size) {
            val id = data.boxType(pos, idSize)
            if (id.isEmpty() || id[0] == '\u0000') break // padding
            val size =
                when {
                    version >= 4 -> syncsafe(data, pos + idSize)
                    version == 3 -> be32(data, pos + idSize)
                    else -> be24(data, pos + idSize)
                }
            val dataStart = pos + headerSize
            if (size <= 0 || dataStart + size > data.size) break
            if (id == "USLT" || id == "ULT") {
                val frame = data.copyOfRange(dataStart, dataStart + size)
                val frameUnsync = version >= 4 && data[pos + 9].toInt() and 0x02 != 0
                usltText(if (frameUnsync) removeUnsync(frame) else frame)?.let { return it }
            }
            pos = dataStart + size
        }
        return ""
    }

    private fun usltText(frame: ByteArray): String? {
        if (frame.size < 5) return null
        val encoding = frame[0].toInt() and 0xFF
        // Locate the NUL-terminated content descriptor at byte level: one NUL
        // for latin1/utf8, an even-aligned NUL pair for UTF-16. Without one the
        // whole payload is treated as lyrics.
        val utf16 = encoding == 0x01 || encoding == 0x02
        var textStart = 4
        var i = 4
        if (utf16) {
            while (i + 1 < frame.size) {
                if (frame[i].toInt() == 0 && frame[i + 1].toInt() == 0) {
                    textStart = i + 2
                    break
                }
                i += 2
            }
        } else {
            while (i < frame.size) {
                if (frame[i].toInt() == 0) {
                    textStart = i + 1
                    break
                }
                i++
            }
        }
        val text =
            when (encoding) {
                0x01 -> decodeUtf16(frame, textStart, frame.size, bigEndian = null)
                0x02 -> decodeUtf16(frame, textStart, frame.size, bigEndian = true)
                0x03 -> frame.decodeToString(textStart, frame.size)
                else -> decodeLatin1(frame, textStart, frame.size)
            }.removePrefix("\uFEFF")
        return text.trim().takeIf { it.isNotEmpty() }
    }

    private fun extractFlac(reader: Reader, head: ByteArray): String {
        // The first metadata block header was consumed together with the sniff.
        var type = head[4].toInt() and 0x7F
        var last = head[4].toInt() and 0x80 != 0
        var size = be24(head, 5)
        var first = true
        repeat(1024) {
            if (!first) {
                val header = reader.readFully(4)
                if (header.size < 4) return ""
                type = header[0].toInt() and 0x7F
                last = header[0].toInt() and 0x80 != 0
                size = be24(header, 1)
            }
            first = false
            if (type == 4) { // VORBIS_COMMENT
                return vorbisCommentLyrics(reader.readFully(minOf(size, MAX_TAG_BYTES)))
            }
            reader.skipFully(size.toLong())
            if (last) return ""
        }
        return ""
    }

    private fun vorbisCommentLyrics(data: ByteArray): String {
        var pos = 0
        fun le32(): Int {
            if (pos + 4 > data.size) return -1
            val v =
                (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8) or
                    ((data[pos + 2].toInt() and 0xFF) shl 16) or ((data[pos + 3].toInt() and 0xFF) shl 24)
            pos += 4
            return v
        }
        val vendorLen = le32()
        if (vendorLen < 0 || pos + vendorLen > data.size) return ""
        pos += vendorLen
        val count = le32()
        if (count < 0) return ""
        repeat(minOf(count, 1024)) {
            val len = le32()
            if (len <= 0 || pos + len > data.size) return ""
            val comment = data.decodeToString(pos, pos + len)
            pos += len
            val eq = comment.indexOf('=')
            if (eq > 0) {
                val key = comment.substring(0, eq)
                if (key.equals("LYRICS", ignoreCase = true) || key.equals("UNSYNCEDLYRICS", ignoreCase = true)) {
                    comment.substring(eq + 1).trim().takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
        }
        return ""
    }

    private fun extractMp4(reader: Reader, head: ByteArray): String {
        // The ftyp header is already consumed with the sniff, skip its content.
        val ftypSize = be32(head, 0).toLong() and 0xFFFFFFFFL
        if (ftypSize < 8L) return ""
        reader.skipFully(ftypSize - 8)
        repeat(64) {
            val header = reader.readFully(8)
            if (header.size < 8) return ""
            val size = be32(header, 0).toLong() and 0xFFFFFFFFL
            val type = header.boxType(4, 4)
            // 64-bit extended box sizes are not worth the plumbing here
            if (size < 8L) return ""
            if (type == "moov") {
                return mp4MoovLyrics(reader.readFully(minOf(size - 8, MAX_MOOV_BYTES.toLong()).toInt()))
            }
            reader.skipFully(size - 8)
        }
        return ""
    }

    private fun mp4MoovLyrics(moov: ByteArray): String {
        val (udtaStart, udtaEnd) = findBox(moov, 0, moov.size, "udta") ?: return ""
        val (metaStart, metaEnd) = findBox(moov, udtaStart, udtaEnd, "meta") ?: return ""
        // meta carries a 4-byte version/flags field before its children
        val (ilstStart, ilstEnd) = findBox(moov, metaStart + 4, metaEnd, "ilst") ?: return ""
        val (lyrStart, lyrEnd) = findBox(moov, ilstStart, ilstEnd, "\u00A9lyr") ?: return ""
        val (dataStart, dataEnd) = findBox(moov, lyrStart, lyrEnd, "data") ?: return ""
        if (dataStart + 8 > dataEnd) return ""
        // data payload: 1-byte type indicator + 3 flags, 4-byte locale, then UTF-8 text
        return moov.decodeToString(dataStart + 8, dataEnd).trim().takeIf { it.isNotEmpty() } ?: ""
    }

    /** Finds a child box in data[from, until) and returns its content range. */
    private fun findBox(data: ByteArray, from: Int, until: Int, type: String): Pair<Int, Int>? {
        var pos = from
        while (pos + 8 <= until) {
            val size = be32(data, pos).toLong() and 0xFFFFFFFFL
            if (size < 8L) return null
            val end = minOf(pos.toLong() + size, until.toLong()).toInt()
            if (data.boxType(pos + 4, 4) == type) return (pos + 8) to end
            pos = end
        }
        return null
    }

    private fun ByteArray.match(offset: Int, expected: String): Boolean {
        if (offset + expected.length > size) return false
        for (i in expected.indices) {
            if (this[offset + i].toInt() != expected[i].code) return false
        }
        return true
    }

    private fun ByteArray.boxType(offset: Int, length: Int): String =
        decodeLatin1(this, offset, offset + length)

    private fun decodeLatin1(data: ByteArray, start: Int, end: Int): String {
        val chars = CharArray(end - start)
        for (i in chars.indices) chars[i] = (data[start + i].toInt() and 0xFF).toChar()
        return chars.concatToString()
    }

    /** bigEndian = null means a leading BOM decides, defaulting to big endian. */
    private fun decodeUtf16(data: ByteArray, start: Int, end: Int, bigEndian: Boolean?): String {
        var from = start
        var big = bigEndian ?: true
        if (bigEndian == null && end - start >= 2) {
            when {
                data[start].toInt() == 0xFF && data[start + 1].toInt() == 0xFE -> {
                    big = false
                    from += 2
                }
                data[start].toInt() == 0xFE && data[start + 1].toInt() == 0xFF -> from += 2
            }
        }
        val sb = StringBuilder((end - from) / 2)
        var i = from
        while (i + 1 < end) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val unit = if (big) (b0 shl 8) or b1 else (b1 shl 8) or b0
            i += 2
            if (unit in 0xD800..0xDBFF && i + 1 < end) {
                val b2 = data[i].toInt() and 0xFF
                val b3 = data[i + 1].toInt() and 0xFF
                val low = if (big) (b2 shl 8) or b3 else (b3 shl 8) or b2
                if (low in 0xDC00..0xDFFF) {
                    i += 2
                    sb.append(0x10000 + ((unit - 0xD800) shl 10) + (low - 0xDC00))
                    continue
                }
            }
            sb.append(unit.toChar())
        }
        return sb.toString()
    }

    private fun be24(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 16) or ((data[offset + 1].toInt() and 0xFF) shl 8) or
            (data[offset + 2].toInt() and 0xFF)

    private fun be32(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 24) or ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)

    private fun syncsafe(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0x7F) shl 21) or ((data[offset + 1].toInt() and 0x7F) shl 14) or
            ((data[offset + 2].toInt() and 0x7F) shl 7) or (data[offset + 3].toInt() and 0x7F)

    private fun removeUnsync(data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var n = 0
        var i = 0
        while (i < data.size) {
            out[n++] = data[i]
            i += if (data[i].toInt() == 0xFF && i + 1 < data.size && data[i + 1].toInt() == 0x00) 2 else 1
        }
        return out.copyOf(n)
    }
}
