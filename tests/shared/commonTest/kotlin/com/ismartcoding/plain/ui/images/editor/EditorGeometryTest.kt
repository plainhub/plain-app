package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.lib.yjs.RectLayer
import kotlin.test.*

class EditorGeometryTest {
    @Test fun unrotatedImageSelectionFollowsItsVisibleBounds() {
        val image = ImageLayer(id = "image", x = 100.0, y = 100.0, w = 200.0, h = 40.0)
        assertTrue(hitTest(image, Point(180.0, 100.0)))
        assertFalse(hitTest(image, Point(100.0, 180.0)))
    }
    @Test fun rotatedImageSelectionFollowsItsVisibleBounds() {
        val image = ImageLayer(id = "image", x = 100.0, y = 100.0, w = 200.0, h = 40.0, rotation = 90.0)
        assertTrue(hitTest(image, Point(100.0, 180.0)))
        assertFalse(hitTest(image, Point(180.0, 100.0)))
    }
    @Test fun hiddenLayersCannotCaptureCanvasSelection() {
        assertFalse(hitTest(RectLayer(id = "hidden", visible = false, x = 0.0, y = 0.0, w = 100.0, h = 100.0), Point(50.0, 50.0)))
    }
    @Test fun rotatedResizeHandlesFollowTheDisplayedCorners() {
        val image = ImageLayer(id = "image", x = 100.0, y = 100.0, w = 200.0, h = 40.0, rotation = 90.0)
        assertEquals(ResizeHandle.TOP_LEFT, hitTestHandle(image, Point(120.0, 0.0), 8.0))
    }
}
