package com.ismartcoding.plain.ui.images.editor

import androidx.lifecycle.ViewModelStore
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.lib.yjs.*
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImageEditorEditViewModelTest {
    @Test
    fun mosaicToolSettingsReachTheCreatedLayer() {
        val layer = createShape(ImageEditorTool.MOSAIC, "fixture", Point(10.0, 20.0), Point(80.0, 90.0), "#ff0000", 4.0, 24.0) as MosaicLayer
        assertEquals(24.0, layer.blockSize)
        assertEquals(70.0, layer.w)
        assertEquals(70.0, layer.h)
    }

    @Test
    fun allLayerTypesKeepDuplicateVisibilityOrderingDeleteAndUndo() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val store = ViewModelStore()
        try {
            val model = ImageEditorEditViewModel(ImageEditorProjectsClient(GraphqlClient { error("Unexpected request") }), dispatcher)
            store.put("editor", model)
            model.loadAsync("")
            val fixtures = listOf(
                ArrowLayer("arrow"), RectLayer("rect"), EllipseLayer("ellipse"),
                HighlightLayer("highlight"), MosaicLayer("mosaic"), TextLayer("text", text = "Fixture"),
                FreehandLayer("brush", points = listOf(Point(1.0, 2.0))), StickerLayer("sticker"),
            )
            fixtures.forEach(model::addLayer)
            model.addImage("generated-image", 100, 80)
            val originalImage = model.layers.last()
            val originals = fixtures + originalImage
            originals.forEach { original ->
                val before = model.document.value
                model.duplicateLayer(original.id)
                val duplicate = model.layers.last()
                assertEquals(original.type, duplicate.type)
                assertEquals(duplicate.id, model.selectedLayerId.value)
                if (original is ImageLayer) assertEquals("generated-image", model.document.value.images[duplicate.id])
                model.toggleVisibility(duplicate.id)
                assertFalse(model.layers.last().visible)
                model.sendBackward(duplicate.id)
                assertEquals(duplicate.id, model.layers[model.layers.lastIndex - 1].id)
                model.bringForward(duplicate.id)
                assertEquals(duplicate.id, model.layers.last().id)
                val beforeDelete = model.document.value
                model.deleteLayer(duplicate.id)
                assertTrue(model.layers.none { it.id == duplicate.id })
                assertFalse(duplicate.id in model.document.value.images)
                model.undo()
                assertEquals(beforeDelete, model.document.value)
                model.redo()
                assertEquals(before, model.document.value)
            }
            val beforeClear = model.document.value
            model.clearLayers()
            assertTrue(model.layers.isEmpty())
            assertTrue(model.document.value.images.isEmpty())
            assertEquals(beforeClear.canvasSize, model.document.value.canvasSize)
            assertEquals(beforeClear.bgColor, model.document.value.bgColor)
            model.undo()
            assertEquals(beforeClear, model.document.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
