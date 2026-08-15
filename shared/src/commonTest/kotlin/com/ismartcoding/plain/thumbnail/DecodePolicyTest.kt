package com.ismartcoding.plain.thumbnail

import kotlin.test.Test
import kotlin.test.assertEquals

class DecodePolicyTest {

    // ── permits: heap budget → concurrent decode permits ───

    @Test
    fun `small heap gets serialized decoding`() {
        assertEquals(2, DecodePolicy.permits(128))
        assertEquals(2, DecodePolicy.permits(64))
    }

    @Test
    fun `mid heap gets four permits`() {
        assertEquals(4, DecodePolicy.permits(192))
        assertEquals(4, DecodePolicy.permits(256))
    }

    @Test
    fun `large heap gets browser connection level parallelism`() {
        assertEquals(6, DecodePolicy.permits(384))
        assertEquals(6, DecodePolicy.permits(512))
    }

    // ── capSampleSize: power-of-2 edge cap ───

    @Test
    fun `image within cap decodes at full resolution`() {
        assertEquals(1, DecodePolicy.capSampleSize(4032, 3024, DecodePolicy.MAX_FULL_VIEW_EDGE))
    }

    @Test
    fun `oversized image halves to fit cap`() {
        assertEquals(2, DecodePolicy.capSampleSize(8000, 6000, DecodePolicy.MAX_FULL_VIEW_EDGE))
    }

    @Test
    fun `huge image quarters to fit cap`() {
        assertEquals(4, DecodePolicy.capSampleSize(16384, 12288, DecodePolicy.MAX_FULL_VIEW_EDGE))
    }

    @Test
    fun `one pixel over the cap triggers sampling`() {
        assertEquals(2, DecodePolicy.capSampleSize(4097, 100, DecodePolicy.MAX_FULL_VIEW_EDGE))
    }

    @Test
    fun `tiny image never samples`() {
        assertEquals(1, DecodePolicy.capSampleSize(100, 100, DecodePolicy.MAX_FULL_VIEW_EDGE))
    }
}
