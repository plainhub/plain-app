package com.ismartcoding.plain.tests

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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.i18n.Res
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
