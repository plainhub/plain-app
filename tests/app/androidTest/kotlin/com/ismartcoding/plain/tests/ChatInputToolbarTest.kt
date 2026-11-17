package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.remove
import com.ismartcoding.plain.i18n.share_folder
import com.ismartcoding.plain.ui.chat.components.ChatInput
import com.ismartcoding.plain.ui.chat.components.FolderShareSheet
import com.ismartcoding.plain.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ChatInputToolbarTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun markdownIconFollowsShareFolderAndShareOpensTheChooser() {
        var shareLabel = ""
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    AppTheme(useDarkTheme = false) {
                        var markdown by remember { mutableStateOf(false) }
                        var sharing by remember { mutableStateOf(false) }
                        val label = stringResource(Res.string.share_folder)
                        SideEffect { shareLabel = label }
                        ChatInput(
                            value = "",
                            attachments = mapOf("fixture" to "fixture.txt"),
                            markdown = markdown,
                            onChangeFormat = { markdown = !markdown },
                            onShareFolder = { sharing = true },
                        )
                        if (sharing) FolderShareSheet(onDismiss = { sharing = false }, onConfirm = { _, _, _ -> })
                    }
                }
            }
            val share = await("Share Folder") { control(it, shareLabel) }
            val markdown = await("Markdown icon") { control(it, "Markdown") }
            assertEquals(share.boundsInRoot.center.y, markdown.boundsInRoot.center.y, 2f)
            assertTrue(markdown.boundsInRoot.left >= share.boundsInRoot.right)
            assertNotNull(markdown.config.getOrNull(SemanticsProperties.ToggleableState))
            assertTrue(nodes().none { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Markdown" } == true })
            click(markdown)
            await("Markdown selected") { control(it, "Markdown")?.takeIf { node -> node.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On } }
            click(await("Share Folder") { control(it, shareLabel) })
            await("folder chooser") { nodes -> nodes.firstOrNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == shareLabel } == true } }
        }
    }

    @Test
    fun showsPickedLocalImageAndKeepsFileNameAndRemoveAction() {
        val context = instrumentation.targetContext
        val file = File(context.cacheDir, "chat-preview-fixture.png")
        Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
            file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file).toString()
        var density = 1f
        var removeLabel = ""
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    density = activity.resources.displayMetrics.density
                    activity.setContent {
                        AppTheme(useDarkTheme = false) {
                            var attachments by remember {
                                mutableStateOf(linkedMapOf(imageUri to "Photo", "fixture" to "Report.pdf").toMap())
                            }
                            val label = stringResource(Res.string.remove)
                            SideEffect { removeLabel = label }
                            ChatInput(
                                value = "",
                                attachments = attachments,
                                imageAttachments = setOf(imageUri),
                                onRemoveAttachment = { attachments = attachments - it },
                            )
                        }
                    }
                }
                val filename = await("image filename") { findText(it, "Photo") }
                await("rendered image thumbnail") {
                    val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: return@await null
                    try {
                        val x = (filename.boundsInWindow.left - 26 * density).toInt()
                        val y = filename.boundsInWindow.center.y.toInt()
                        filename.takeIf { x in 0 until screenshot.width && y in 0 until screenshot.height && screenshot.getPixel(x, y) == Color.MAGENTA }
                    } finally { screenshot.recycle() }
                }
                await("generic file remains named") { findText(it, "Report.pdf") }
                click(await("remove action") { nodes ->
                    findText(nodes, removeLabel)?.let { generateSequence(it) { node -> node.parent }.firstOrNull { node -> node.config.getOrNull(SemanticsActions.OnClick) != null } }
                })
                await("only selected image removed") { nodes ->
                    findText(nodes, "Report.pdf")?.takeIf { findText(nodes, "Photo") == null }
                }
            }
        } finally {
            file.delete()
        }
    }

    private fun findText(nodes: List<SemanticsNode>, label: String): SemanticsNode? =
        nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true }

    private fun control(nodes: List<SemanticsNode>, label: String): SemanticsNode? =
        nodes.firstOrNull { label in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
            ?.let { generateSequence(it) { node -> node.parent }.firstOrNull { node -> node.config.getOrNull(SemanticsActions.OnClick) != null } }

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
            result = WindowInspector.getGlobalWindowViews().filter { it.isShown }.flatMap(::roots)
                .flatMap { flatten(it.semanticsOwner.unmergedRootSemanticsNode) }
        }
        return result
    }

    private fun await(expected: String, find: (List<SemanticsNode>) -> SemanticsNode?): SemanticsNode {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            find(nodes())?.let { return it }
            Thread.sleep(25)
        }
        error("Expected $expected")
    }
}
