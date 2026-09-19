package com.ismartcoding.plain.tests

import com.ismartcoding.plain.ui.page.scan.components.FitRect
import com.ismartcoding.plain.ui.page.scan.components.ScanImageLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanImageLayoutTest {

    @Test
    fun fitRectScalesToTheLimitingSide() {
        // landscape image in a portrait view: width fits exactly, height letterboxed
        val rect = ScanImageLayout.fitRect(720f, 1280f, 1920f, 1080f)
        assertEquals(720f, rect.width)
        assertEquals(1080f * 720f / 1920f, rect.height)
        assertEquals(0f, rect.left)
        assertEquals((1280f - rect.height) / 2f, rect.top)
    }

    @Test
    fun fitRectSameAspectFillsTheView() {
        val rect = ScanImageLayout.fitRect(1080f, 1920f, 540f, 960f)
        assertEquals(FitRect(0f, 0f, 1080f, 1920f), rect)
    }

    @Test
    fun fitRectDegeneratesToTheView() {
        val rect = ScanImageLayout.fitRect(500f, 400f, 0f, 100f)
        assertEquals(FitRect(0f, 0f, 500f, 400f), rect)
    }

    @Test
    fun tagCenterMapsImagePointIntoFittedRect() {
        // image center lands in the view center
        val rect = ScanImageLayout.fitRect(720f, 1280f, 1920f, 1080f)
        val (cx, cy) = ScanImageLayout.tagCenterIn(0.5f, 0.5f, rect, 720f, 1280f, 40f)
        assertEquals(360f, cx)
        assertEquals(640f, cy)
    }

    @Test
    fun tagCenterClampsToKeepTheTagInsideTheView() {
        // same-aspect rect fills the view, so image corners map to view corners and clamp
        val rect = ScanImageLayout.fitRect(720f, 1280f, 360f, 640f)
        val (cx, cy) = ScanImageLayout.tagCenterIn(0f, 0f, rect, 720f, 1280f, 40f)
        assertEquals(40f, cx)
        assertEquals(40f, cy)
        val (ex, ey) = ScanImageLayout.tagCenterIn(1f, 1f, rect, 720f, 1280f, 40f)
        assertEquals(680f, ex)
        assertEquals(1240f, ey)
    }
}
