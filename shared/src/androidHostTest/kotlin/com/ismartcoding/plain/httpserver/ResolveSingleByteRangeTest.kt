package com.ismartcoding.plain.httpserver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResolveSingleByteRangeTest {
    private val fileLength = 100L

    private fun full() = ResolvedFileRange(0, fileLength - 1, isPartial = false)

    private fun resolve(header: String?, length: Long = fileLength) =
        resolveSingleByteRange(header, length)

    @Test
    fun nullHeader_returnsFullRange() {
        assertEquals(full(), resolve(null))
    }

    @Test
    fun blankHeader_returnsFullRange() {
        assertEquals(full(), resolve("  "))
    }

    @Test
    fun openEndedRange_returnsPartialToEnd() {
        val range = resolve("bytes=10-")!!
        assertTrue(range.isPartial)
        assertEquals(10L, range.start)
        assertEquals(99L, range.endInclusive)
        assertEquals(90L, range.length)
    }

    @Test
    fun boundedRange_returnsExactSlice() {
        val range = resolve("bytes=10-19")!!
        assertTrue(range.isPartial)
        assertEquals(10L, range.start)
        assertEquals(19L, range.endInclusive)
        assertEquals(10L, range.length)
    }

    @Test
    fun suffixRange_returnsLastNBytes() {
        val range = resolve("bytes=-50")!!
        assertTrue(range.isPartial)
        assertEquals(50L, range.start)
        assertEquals(99L, range.endInclusive)
        assertEquals(50L, range.length)
    }

    @Test
    fun suffixLargerThanFile_returnsWholeFile() {
        val range = resolve("bytes=-9999")!!
        assertTrue(range.isPartial)
        assertEquals(0L, range.start)
        assertEquals(99L, range.endInclusive)
    }

    @Test
    fun endBeyondFileSize_isClamped() {
        val range = resolve("bytes=0-9999")!!
        assertEquals(99L, range.endInclusive)
        assertEquals(100L, range.length)
    }

    @Test
    fun startAtFileSize_returns416() {
        assertNull(resolve("bytes=100-"))
    }

    @Test
    fun startBeyondFileSize_returns416() {
        assertNull(resolve("bytes=500-"))
    }

    @Test
    fun endBeforeStart_returns416() {
        assertNull(resolve("bytes=50-49"))
    }

    @Test
    fun zeroSuffix_returns416() {
        assertNull(resolve("bytes=-0"))
    }

    @Test
    fun emptyFile_withRange_returns416() {
        assertNull(resolve("bytes=0-", length = 0L))
    }

    @Test
    fun emptyFile_withoutRange_returnsEmptyFullRange() {
        val range = resolve(null, length = 0L)!!
        assertFalse(range.isPartial)
        assertEquals(0L, range.length)
    }

    @Test
    fun multiRange_isIgnored_returnsFullRange() {
        assertEquals(full(), resolve("bytes=0-1,5-6"))
    }

    @Test
    fun nonBytesUnit_isIgnored_returnsFullRange() {
        assertEquals(full(), resolve("items=0-1"))
    }

    @Test
    fun missingDash_isIgnored_returnsFullRange() {
        assertEquals(full(), resolve("bytes=50"))
    }

    @Test
    fun emptySpec_isIgnored_returnsFullRange() {
        assertEquals(full(), resolve("bytes="))
        assertEquals(full(), resolve("bytes=-"))
    }

    @Test
    fun malformedNumbers_areIgnored_returnsFullRange() {
        assertEquals(full(), resolve("bytes=abc-def"))
        assertEquals(full(), resolve("bytes=10-x"))
        assertEquals(full(), resolve("bytes=-x"))
    }

    @Test
    fun negativeStart_isIgnored_returnsFullRange() {
        assertEquals(full(), resolve("bytes=-1-5"))
    }

    @Test
    fun unitKeyword_isCaseInsensitive() {
        val range = resolve("BYTES=0-9")!!
        assertTrue(range.isPartial)
        assertEquals(0L, range.start)
        assertEquals(9L, range.endInclusive)
    }

    @Test
    fun whitespace_isTolerated() {
        val range = resolve(" bytes= 10 - 20 ")!!
        assertTrue(range.isPartial)
        assertEquals(10L, range.start)
        assertEquals(20L, range.endInclusive)
    }
}
