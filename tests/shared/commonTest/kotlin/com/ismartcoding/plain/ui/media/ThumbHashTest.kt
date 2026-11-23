package com.ismartcoding.plain.ui.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThumbHashTest {
    @Test
    fun rendersLandscapeAndAlphaWithTheirEmbeddedAspectRatios() {
        val landscape = assertNotNull(decodeThumbHash("UUgKFZJwd3eCh4eHiHiIh4AZCMd3"))
        assertEquals(32, landscape.width)
        assertEquals(23, landscape.height)
        assertTrue(landscape.rgba.filterIndexed { index, _ -> index % 4 == 3 }.all { it == 255.toByte() })
        val portrait = assertNotNull(decodeThumbHash("U9iFCwgYgaCXd4h7ARn4aHN3cIiHeHg="))
        assertEquals(19, portrait.width)
        assertEquals(32, portrait.height)
        assertEquals(listOf(46, 63, 55, 77), portrait.rgba.take(4).map { it.toInt() and 255 })
        assertTrue(portrait.rgba.filterIndexed { index, _ -> index % 4 == 3 }.distinct().size > 1)
    }

    @Test
    fun rejectsTruncatedAndInvalidHashes() {
        assertNull(decodeThumbHash("UUgKFZJwd3eCiIiHiHiI"))
        assertNull(decodeThumbHash("!!!!!!!!"))
        assertNull(decodeThumbHash("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
    }
}
