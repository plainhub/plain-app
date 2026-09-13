package com.ismartcoding.plain.lib

import kotlin.test.Test
import kotlin.test.assertEquals

class EmbeddedLyricsTest {
    private class ByteArrayReader(data: ByteArray) : EmbeddedLyrics.Reader() {
        private var pos = 0
        private val bytes = data

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (pos >= bytes.size) return -1
            val n = minOf(length, bytes.size - pos)
            bytes.copyInto(buffer, offset, pos, pos + n)
            pos += n
            return n
        }

        override fun close() {}
    }

    private fun extract(vararg chunks: ByteArray): String =
        EmbeddedLyrics.extract(ByteArrayReader(chunks.reduce { a, b -> a + b }))

    private fun be32(size: Int) =
        byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte())

    private fun be24(size: Int) = byteArrayOf((size shr 16).toByte(), (size shr 8).toByte(), size.toByte())

    private fun le32(size: Int) =
        byteArrayOf(size.toByte(), (size shr 8).toByte(), (size shr 16).toByte(), (size shr 24).toByte())

    private fun syncsafe(size: Int) =
        byteArrayOf(
            ((size shr 21) and 0x7F).toByte(),
            ((size shr 14) and 0x7F).toByte(),
            ((size shr 7) and 0x7F).toByte(),
            (size and 0x7F).toByte(),
        )

    // ISO-8859-1 so box types like ©lyr stay single-byte, matching real files
    private fun box(type: String, content: ByteArray) =
        be32(content.size + 8) + type.toByteArray(Charsets.ISO_8859_1) + content

    @Test
    fun id3v23Utf8Uslt() {
        val lyrics = "[00:01.00]Hello\n[00:05.00]World"
        val frameBody = byteArrayOf(3) + "eng".encodeToByteArray() + byteArrayOf(0) + lyrics.encodeToByteArray()
        val frame = "USLT".encodeToByteArray() + be32(frameBody.size) + byteArrayOf(0, 0) + frameBody
        val header = "ID3".encodeToByteArray() + byteArrayOf(3, 0, 0) + syncsafe(frame.size)
        assertEquals(lyrics, extract(header + frame))
    }

    @Test
    fun id3v24Utf16Uslt() {
        val lyrics = "同步歌词"
        val encoded = lyrics.toByteArray(Charsets.UTF_16) // includes BOM
        val frameBody = byteArrayOf(1) + "chi".encodeToByteArray() + byteArrayOf(0, 0) + encoded
        val frame = "USLT".encodeToByteArray() + syncsafe(frameBody.size) + byteArrayOf(0, 0) + frameBody
        val header = "ID3".encodeToByteArray() + byteArrayOf(4, 0, 0) + syncsafe(frame.size)
        assertEquals(lyrics, extract(header + frame))
    }

    @Test
    fun id3UsltSkipsDescriptor() {
        val lyrics = "plain text lyrics"
        val frameBody =
            byteArrayOf(0) + "eng".encodeToByteArray() + "desc".encodeToByteArray() + byteArrayOf(0) + lyrics.encodeToByteArray()
        val frame = "USLT".encodeToByteArray() + be32(frameBody.size) + byteArrayOf(0, 0) + frameBody
        val header = "ID3".encodeToByteArray() + byteArrayOf(3, 0, 0) + syncsafe(frame.size)
        assertEquals(lyrics, extract(header + frame))
    }

    @Test
    fun id3WithoutLyricsReturnsEmpty() {
        val frameBody = byteArrayOf(0) + "title".encodeToByteArray() + byteArrayOf(0)
        val frame = "TIT2".encodeToByteArray() + be32(frameBody.size) + byteArrayOf(0, 0) + frameBody
        val header = "ID3".encodeToByteArray() + byteArrayOf(3, 0, 0) + syncsafe(frame.size)
        assertEquals("", extract(header + frame))
    }

    @Test
    fun flacVorbisCommentLyrics() {
        val lyrics = "[00:10.00] lyrics line"
        val comment = "LYRICS=$lyrics".encodeToByteArray()
        val block = le32(3) + "ref".encodeToByteArray() + le32(1) + le32(comment.size) + comment
        val data =
            "fLaC".encodeToByteArray() +
                byteArrayOf(0) + be24(18) + ByteArray(18) + // STREAMINFO, not last
                byteArrayOf(0x84.toByte()) + be24(block.size) + block // VORBIS_COMMENT, last
        assertEquals(lyrics, extract(data))
    }

    @Test
    fun flacUnsyncedlyricsKeyAccepted() {
        val lyrics = "flac lyrics"
        val comment = "UNSYNCEDLYRICS=$lyrics".encodeToByteArray()
        val block = le32(0) + le32(1) + le32(comment.size) + comment
        val data = "fLaC".encodeToByteArray() + byteArrayOf(0x84.toByte()) + be24(block.size) + block
        assertEquals(lyrics, extract(data))
    }

    @Test
    fun mp4CopyleftLyrAtom() {
        val lyrics = "m4a embedded lyrics"
        val lyrData =
            box("data", byteArrayOf(0, 0, 0, 1) + byteArrayOf(0, 0, 0, 0) + lyrics.encodeToByteArray())
        val ilst = box("ilst", box("\u00A9lyr", lyrData))
        val moov = box("moov", box("udta", box("meta", byteArrayOf(0, 0, 0, 0) + ilst)))
        val ftyp = box("ftyp", "M4A ".encodeToByteArray() + byteArrayOf(0, 0, 0, 0))
        assertEquals(lyrics, extract(ftyp + moov))
    }

    @Test
    fun mp4WithoutLyricsReturnsEmpty() {
        val moov = box("moov", box("udta", box("meta", byteArrayOf(0, 0, 0, 0) + box("ilst", box("\u00A9nam", box("data", byteArrayOf(0, 0, 0, 1) + byteArrayOf(0, 0, 0, 0) + "song".encodeToByteArray()))))))
        val ftyp = box("ftyp", "M4A ".encodeToByteArray() + byteArrayOf(0, 0, 0, 0))
        assertEquals("", extract(ftyp + moov))
    }

    @Test
    fun plainMp3WithoutId3ReturnsEmpty() {
        assertEquals("", extract(byteArrayOf(0xFF.toByte(), 0xFB.toByte()) + ByteArray(64)))
    }

    @Test
    fun tinyFileReturnsEmpty() {
        assertEquals("", extract("ID".encodeToByteArray()))
    }
}
