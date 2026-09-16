package com.ismartcoding.plain.lib.codeeditor

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Long-line chunked display: the document layer behind no-wrap rendering of huge lines. */
class LongLineChunkTest {

    private fun docOf(content: String): LineVectorDocument =
        LineIndexBuilder(ByteArrayByteSource(content.encodeToByteArray())).buildAll()

    @Test
    fun chunkCountForLongLine() {
        val doc = docOf("short\n" + "x".repeat(4501) + "\ntail")
        assertEquals(1, doc.lineChunkCount(0))
        assertEquals(3, doc.lineChunkCount(1)) // 4501 bytes -> ceil(4501/2000)
        assertEquals(1, doc.lineChunkCount(2))
    }

    @Test
    fun chunksConcatenateToOriginalLine() {
        val random = Random(7)
        val longLine = buildString {
            repeat(9000) {
                // Mix ASCII and CJK so UTF-8 chunk boundaries get exercised.
                append(if (random.nextInt(4) == 0) '好' else ('a' + random.nextInt(26)))
            }
        }
        val doc = docOf("head\n$longLine\ntail")
        val rebuilt = (0 until doc.lineChunkCount(1)).joinToString("") { doc.lineChunkText(1, it) }
        assertEquals(longLine, rebuilt)
        // Char starts are monotonically increasing and map back to the same global offset.
        var expected = 0
        for (c in 0 until doc.lineChunkCount(1)) {
            assertEquals(expected, doc.lineChunkCharStart(1, c))
            expected += doc.lineChunkText(1, c).length
        }
        assertEquals(longLine.length, expected)
    }

    @Test
    fun editedLongLineChunksStayConsistent() {
        val doc = docOf("x".repeat(5000))
        doc.insert(0, 100, "你好") // edited line overrides file-backed bytes
        // Chunk count is byte-based for both file-backed and edited lines (5002 chars
        // encode to 5004 bytes -> 3 chunks of ~2000 bytes each).
        assertEquals(3, doc.lineChunkCount(0))
        val rebuilt = (0 until doc.lineChunkCount(0)).joinToString("") { doc.lineChunkText(0, it) }
        assertEquals("x".repeat(100) + "你好" + "x".repeat(4900), rebuilt)
    }

    @Test
    fun chunkTextRandomAgainstFullLine() {
        val random = Random(11)
        repeat(10) { seed ->
            val len = random.nextInt(1, 6000)
            val line = buildString { repeat(len) { append(('a' + random.nextInt(26))) } }
            val doc = docOf(line)
            val rebuilt = (0 until doc.lineChunkCount(0)).joinToString("") { doc.lineChunkText(0, it) }
            assertTrue(rebuilt == line, "seed $seed chunked text diverged")
        }
    }
}
