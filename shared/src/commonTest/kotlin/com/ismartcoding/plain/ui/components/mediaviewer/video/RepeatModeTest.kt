package com.ismartcoding.plain.ui.components.mediaviewer.video

import kotlin.test.Test
import kotlin.test.assertEquals

class RepeatModeTest {

    @Test
    fun `toRepeatMode maps 0 to NONE`() {
        assertEquals(RepeatMode.NONE, 0.toRepeatMode())
    }

    @Test
    fun `toRepeatMode maps 1 to ONE`() {
        assertEquals(RepeatMode.ONE, 1.toRepeatMode())
    }

    @Test
    fun `toRepeatMode maps 2 to ALL`() {
        assertEquals(RepeatMode.ALL, 2.toRepeatMode())
    }

    @Test
    fun `toRepeatMode maps out-of-range values to NONE`() {
        assertEquals(RepeatMode.NONE, (-1).toRepeatMode())
        assertEquals(RepeatMode.NONE, 3.toRepeatMode())
        assertEquals(RepeatMode.NONE, 100.toRepeatMode())
    }

    @Test
    fun `enum values have correct string representations`() {
        assertEquals("none", RepeatMode.NONE.value)
        assertEquals("one", RepeatMode.ONE.value)
        assertEquals("all", RepeatMode.ALL.value)
    }
}
