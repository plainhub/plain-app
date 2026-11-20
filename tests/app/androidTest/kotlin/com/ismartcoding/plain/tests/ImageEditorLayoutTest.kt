package com.ismartcoding.plain.tests

import android.view.PixelCopy
import android.view.Window
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.images.editor.*
import com.ismartcoding.plain.ui.navigation.MainDialogs
import com.ismartcoding.plain.ui.theme.AppTheme
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageEditorLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var window: Window

    @Test
    fun realPageKeepsToolsLayerActionsAndExportWithinNarrowScreen() {
        val client = ImageEditorProjectsClient(ContentSource.Local.api)
        val vm = ImageEditorEditViewModel(client)
        val store = ViewModelStore()
        val labels = mutableMapOf<String, String>()
        val dark = mutableStateOf(false)
        val large = mutableStateOf(false)
        var density = 1f
        instrumentation.runOnMainSync { store.put("editor", vm) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    window = activity.window
                    activity.setContent {
                        AppTheme(useDarkTheme = dark.value) {
                            val original = LocalDensity.current
                            SideEffect { density = original.density }
                            CompositionLocalProvider(LocalDensity provides Density(original.density, if (large.value) 1.5f else 1f)) {
                                val names = mapOf(
                                    "select" to stringResource(Res.string.image_editor_tool_select),
                                    "brush" to stringResource(Res.string.image_editor_tool_freehand),
                                    "highlight" to stringResource(Res.string.image_editor_tool_highlight),
                                    "shapes" to stringResource(Res.string.image_editor_shapes),
                                    "arrow" to stringResource(Res.string.image_editor_tool_arrow),
                                    "ellipse" to stringResource(Res.string.image_editor_tool_ellipse),
                                    "rect" to stringResource(Res.string.image_editor_tool_rect),
                                    "text" to stringResource(Res.string.image_editor_tool_text),
                                    "mosaic" to stringResource(Res.string.image_editor_tool_mosaic),
                                    "more" to stringResource(Res.string.more),
                                    "export" to stringResource(Res.string.image_editor_export),
                                    "copyImage" to stringResource(Res.string.image_editor_copy_image),
                                    "layers" to stringResource(Res.string.image_editor_layers),
                                    "style" to stringResource(Res.string.style),
                                    "edit" to stringResource(Res.string.edit),
                                    "duplicate" to stringResource(Res.string.image_editor_duplicate_layer),
                                    "order" to stringResource(Res.string.image_editor_layer_order),
                                    "delete" to stringResource(Res.string.delete),
                                    "undo" to stringResource(Res.string.undo),
                                    "redo" to stringResource(Res.string.redo),
                                    "done" to stringResource(Res.string.done),
                                    "close" to stringResource(Res.string.close),
                                    "bring" to stringResource(Res.string.image_editor_bring_forward),
                                    "send" to stringResource(Res.string.image_editor_send_backward),
                                    "visibility" to stringResource(Res.string.image_editor_layer_visibility),
                                    "addImage" to stringResource(Res.string.image_editor_add_image),
                                    "sticker" to stringResource(Res.string.image_editor_tool_sticker),
                                    "crop" to stringResource(Res.string.image_editor_crop),
                                    "canvas" to stringResource(Res.string.image_editor_canvas_settings),
                                    "preview" to stringResource(Res.string.image_editor_preview),
                                    "font" to stringResource(Res.string.image_editor_font_family),
                                    "glow" to stringResource(Res.string.image_editor_glow),
                                    "clear" to stringResource(Res.string.image_editor_clear_all),
                                    "cancel" to stringResource(Res.string.cancel),
                                    "confirm" to stringResource(Res.string.confirm),
                                )
                                SideEffect { labels.putAll(names) }
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Box(Modifier.width(320.dp).fillMaxHeight()) { ImageEditorPage(rememberNavController(), "", vm = vm) }
                                }
                                MainDialogs()
                            }
                        }
                    }
                }
                fun button(key: String) = await(key) { nodes -> labels[key]?.let { button(nodes, it) } }
                fun tap(key: String) = click(button(key))
                fun close() = tap("close")
                fun baseTools() {
                    val tools = listOf("select", "brush", "shapes", "text", "mosaic", "more").map { button(it) }
                    tools.zipWithNext().forEach { (a, b) ->
                        val sameRow = kotlin.math.abs(a.boundsInRoot.top - b.boundsInRoot.top) < 1f
                        assertTrue("Tool targets overlap", if (sameRow) a.boundsInRoot.right <= b.boundsInRoot.left + 1f else a.boundsInRoot.bottom <= b.boundsInRoot.top + 1f)
                    }
                    tools.forEach { assertTrue("Tool target must remain 48dp wide", it.boundsInRoot.width >= 48f * density - 1f) }
                    listOf("select", "brush", "shapes", "text", "mosaic", "more").forEach { key ->
                        val node = checkNotNull(text(nodes(), labels.getValue(key)))
                        val layouts = mutableListOf<TextLayoutResult>()
                        instrumentation.runOnMainSync { assertTrue(node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts) == true) }
                        assertEquals("Tool label must remain complete: $key", 1, layouts.single().lineCount)
                    }
                }
                baseTools()
                val normalToolHeight = button("select").boundsInRoot.height
                capture("plain-editor-tools.png")
                val exportTop = button("export").boundsInRoot.top
                assertTrue(exportTop < button("undo").boundsInRoot.top)
                tap("shapes"); button("arrow"); button("rect"); tap("ellipse")
                assertEquals(ImageEditorTool.ELLIPSE, vm.currentTool.value)
                tap("brush"); tap("highlight")
                assertEquals(ImageEditorTool.HIGHLIGHT, vm.currentTool.value)
                tap("mosaic"); tap("style"); close()
                tap("more"); button("addImage"); button("sticker"); button("crop"); button("canvas"); button("preview"); close()
                tap("export"); listOf("PNG", "JPEG", "WEBP").forEach { label -> await(label) { button(it, label) } }; button("copyImage"); close()
                instrumentation.runOnMainSync { vm.currentTool.value = ImageEditorTool.SELECT; vm.addLayer(TextLayer("layout-fixture", text = "Editor fixture", x = 640.0, y = 400.0, fontSize = 96.0, maxWidth = 640.0)) }
                button("edit"); button("duplicate"); button("delete")
                assertEquals(exportTop, button("export").boundsInRoot.top, 1f)
                assertTrue(button("delete").boundsInRoot.top > button("undo").boundsInRoot.top)
                tap("style")
                await("Font family") { text(it, labels.getValue("font")) }
                await("Glow") { text(it, labels.getValue("glow")) }
                close()
                tap("edit"); tap("cancel")
                tap("duplicate")
                assertEquals(2, vm.layers.size)
                val duplicate = vm.selectedLayerId.value
                tap("order"); tap("send")
                assertEquals(duplicate, vm.layers.first().id)
                tap("visibility"); assertFalse(vm.layers.first().visible); close()
                capture("plain-editor-selected.png")
                tap("delete"); assertEquals(1, vm.layers.size)
                tap("undo"); assertEquals(2, vm.layers.size)
                tap("redo"); assertEquals(1, vm.layers.size)
                tap("layers")
                click(await("Fixture layer") { button(it, "Editor fixture") })
                assertEquals("layout-fixture", vm.selectedLayerId.value)
                tap("done"); baseTools()
                tap("layers"); tap("clear"); tap("cancel"); assertEquals(1, vm.layers.size)
                tap("layers"); tap("clear"); tap("confirm"); assertTrue(vm.layers.isEmpty())
                tap("undo"); assertEquals(1, vm.layers.size)
                instrumentation.runOnMainSync { dark.value = true; large.value = true }
                await("Large text layout") { nodes -> button(nodes, labels.getValue("select"))?.takeIf { it.boundsInRoot.height > normalToolHeight } }
                baseTools()
                capture("plain-editor-dark-large.png")
            }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            if (vm.id.value.isNotEmpty()) runBlocking { client.deleteAsync(vm.id.value) }
        }
    }

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = android.graphics.Bitmap.createBitmap(window.decorView.width, window.decorView.height, android.graphics.Bitmap.Config.ARGB_8888)
        val result = CompletableFuture<Int>()
        instrumentation.runOnMainSync { PixelCopy.request(window, bitmap, { result.complete(it) }, Handler(Looper.getMainLooper())) }
        assertEquals(PixelCopy.SUCCESS, result.get(3, TimeUnit.SECONDS).toInt())
        File(instrumentation.targetContext.cacheDir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun text(nodes: List<SemanticsNode>, label: String): SemanticsNode? = nodes.firstOrNull {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
    }

    private fun button(nodes: List<SemanticsNode>, label: String): SemanticsNode? = nodes.asSequence().filter {
        it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true || label in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    }.mapNotNull { generateSequence(it) { node -> node.parent }.firstOrNull { node -> node.config.getOrNull(SemanticsActions.OnClick) != null } }.firstOrNull()

    private fun click(node: SemanticsNode) = instrumentation.runOnMainSync {
        assertTrue(node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke() == true)
    }

    private fun nodes(): List<SemanticsNode> {
        fun roots(view: View): List<ViewRootForTest> = when (view) {
            is ViewRootForTest -> listOf(view)
            is ViewGroup -> (0 until view.childCount).flatMap { roots(view.getChildAt(it)) }
            else -> emptyList()
        }
        fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)
        var result = emptyList<SemanticsNode>()
        instrumentation.runOnMainSync {
            result = WindowInspector.getGlobalWindowViews().filter { it.isShown }.flatMap(::roots).flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
        }
        return result
    }

    private fun await(expected: String, find: (List<SemanticsNode>) -> SemanticsNode?): SemanticsNode {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) { find(nodes())?.let { return it }; Thread.sleep(25) }
        error("Expected $expected")
    }
}
