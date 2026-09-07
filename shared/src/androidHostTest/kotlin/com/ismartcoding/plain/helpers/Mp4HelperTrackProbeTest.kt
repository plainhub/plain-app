package com.ismartcoding.plain.helpers

import java.io.File
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Mp4HelperTrackProbeTest {

    private fun box(type: String, vararg payloads: ByteArray): ByteArray {
        val size = 8 + payloads.sumOf { it.size }
        val out = ByteBuffer.allocate(size)
        out.putInt(size)
        out.put(type.toByteArray(Charsets.US_ASCII))
        payloads.forEach { out.put(it) }
        return out.array()
    }

    private fun trak(sampleEntry: String?): ByteArray {
        val hdlr = box("hdlr", ByteArray(16))
        val mediaBoxes = if (sampleEntry != null) {
            val entry = box(sampleEntry, ByteArray(20))
            val stsd = box("stsd", byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1), entry)
            arrayOf(hdlr, box("minf", box("stbl", stsd)))
        } else {
            arrayOf(hdlr)
        }
        val tkhd = box("tkhd", ByteArray(16))
        return box("trak", tkhd, box("mdia", *mediaBoxes))
    }

    private fun writeTemp(vararg topBoxes: ByteArray): File {
        val file = File.createTempFile("probe_", ".mp4")
        file.outputStream().use { out -> topBoxes.forEach { out.write(it) } }
        file.deleteOnExit()
        return file
    }

    private fun mebxFile(): File {
        val ftyp = box("ftyp", ByteArray(8))
        val mdat = box("mdat", ByteArray(64))
        val moov = box("moov", trak("avc1"), trak("mp4a"), trak("mebx"))
        return writeTemp(ftyp, mdat, moov)
    }

    private fun cleanFile(): File {
        val ftyp = box("ftyp", ByteArray(8))
        val moov = box("moov", trak("avc1"), trak("mp4a"))
        val mdat = box("mdat", ByteArray(64))
        return writeTemp(ftyp, moov, mdat)
    }

    @Test
    fun mebxTrack_isDetected() {
        assertTrue(Mp4Helper.hasBrowserIncompatibleTrack(mebxFile().absolutePath))
    }

    @Test
    fun audioVideoOnly_isNotDetected() {
        assertFalse(Mp4Helper.hasBrowserIncompatibleTrack(cleanFile().absolutePath))
    }

    @Test
    fun secondMebxTrack_isDetected() {
        val moov = box("moov", trak("mp4a"), trak("hvc1"), trak("mebx"))
        val file = writeTemp(box("ftyp", ByteArray(8)), box("mdat", ByteArray(16)), moov)
        assertTrue(Mp4Helper.hasBrowserIncompatibleTrack(file.absolutePath))
    }

    @Test
    fun missingFile_isNotDetected() {
        assertFalse(Mp4Helper.hasBrowserIncompatibleTrack("/nonexistent/x.mp4"))
    }

    @Test
    fun garbageFile_isNotDetected() {
        val file = writeTemp(ByteArray(64))
        assertFalse(Mp4Helper.hasBrowserIncompatibleTrack(file.absolutePath))
    }
}
