package com.ismartcoding.plain.lib.codeeditor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HorizontalPanTest {

    private val pan = HorizontalPan()

    @Test
    fun dragLeftPansRightUpToMax() {
        pan.updateBounds(contentWidthPx = 5000f, viewportWidthPx = 1000f)
        assertTrue(pan.drag(-400f), "dragging left should change offset")
        assertEquals(400f, pan.offsetPx)
        pan.drag(-200f)
        assertEquals(600f, pan.offsetPx)
        // Exceeding the max clamps.
        pan.drag(-100000f)
        assertEquals(4000f, pan.offsetPx)
    }

    @Test
    fun dragRightPansBackAndStopsAtZero() {
        pan.updateBounds(5000f, 1000f)
        pan.drag(-1500f)
        assertTrue(pan.drag(500f))
        assertEquals(1000f, pan.offsetPx)
        assertTrue(pan.drag(1000f))
        assertEquals(0f, pan.offsetPx)
        assertFalse(pan.drag(500f), "panning right at 0 must not change offset")
        assertEquals(0f, pan.offsetPx)
    }

    @Test
    fun viewportWiderThanContentLocksOffsetAtZero() {
        pan.updateBounds(800f, 1000f)
        assertEquals(0f, pan.maxOverhangPx)
        assertFalse(pan.drag(-50f))
        assertEquals(0f, pan.offsetPx)
    }

    @Test
    fun updateBoundsClampsOffsetWhenContentShrinks() {
        pan.updateBounds(5000f, 1000f)
        pan.drag(-3900f)
        assertEquals(3900f, pan.offsetPx)
        // Content shrinks (narrower widest row discovered): offset must follow the bound.
        pan.updateBounds(2000f, 1000f)
        assertEquals(1000f, pan.offsetPx)
        pan.updateBounds(500f, 1000f)
        assertEquals(0f, pan.offsetPx)
    }

    @Test
    fun accumulatingDragsCompose() {
        pan.updateBounds(3000f, 1000f)
        repeat(10) { pan.drag(-100f) }
        assertEquals(1000f, pan.offsetPx)
    }

    @Test
    fun zeroWidthContentAndViewport() {
        pan.updateBounds(0f, 0f)
        assertFalse(pan.drag(-10f))
        assertEquals(0f, pan.offsetPx)
    }
}
