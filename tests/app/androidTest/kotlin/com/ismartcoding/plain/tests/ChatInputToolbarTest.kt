package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
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
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.chat.ChatDraftAttachment
import com.ismartcoding.plain.features.chat.models.ChatAttachmentType
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.chat_input_hint
import com.ismartcoding.plain.i18n.close
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.onboarding_skip
import com.ismartcoding.plain.i18n.pause
import com.ismartcoding.plain.i18n.remove
import com.ismartcoding.plain.i18n.share_folder
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.chat.components.ChatInput
import com.ismartcoding.plain.ui.chat.components.FolderShareSheet
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
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
                            previewerState = rememberPreviewerState(),
                            attachments = mapOf("fixture" to ChatDraftAttachment(ChatAttachmentType.FILE, "fixture.txt")),
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
    fun previewsPickedMediaWithoutShowingNamesAndKeepsOrdinaryFileAndRemoveAction() {
        val context = instrumentation.targetContext
        val image = File.createTempFile("chat-preview-image-", ".png", context.cacheDir)
        val video = File.createTempFile("chat-preview-video-", ".mp4", context.cacheDir)
        val document = File.createTempFile("chat-preview-document-", ".txt", context.cacheDir)
        fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.provider", file).toString()
        Bitmap.createBitmap(240, 120, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
            image.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        try {
            requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                video.outputStream().use { input.copyTo(it) }
            }
            document.writeText("Draft attachment test")
            ActivityScenario.launch(MainActivity::class.java).use {
                val localChat = LocaleHelper.getString(Res.string.local_chat)
                val skip = LocaleHelper.getString(Res.string.onboarding_skip)
                val hint = LocaleHelper.getString(Res.string.chat_input_hint)
                val close = LocaleHelper.getString(Res.string.close)
                val pause = LocaleHelper.getString(Res.string.pause)
                val remove = LocaleHelper.getString(Res.string.remove)
                await("local conversation or onboarding") { findText(it, skip) ?: findText(it, localChat) }
                findText(nodes(), skip)?.let(::clickable)?.let(::click)
                click(await("local conversation") { nodes -> findText(nodes, localChat)?.let(::clickable) })
                await("chat composer") { nodes ->
                    findText(nodes, hint) ?: nodes.firstOrNull { it.config.getOrNull(SemanticsActions.SetText) != null }
                }
                instrumentation.waitForIdleSync()
                sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, linkedSetOf(uri(image), uri(video))))
                sendEvent(PickFileResultEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, setOf(uri(document))))
                await("ordinary file name") { findText(it, document.name) }
                assertTrue("Image filename should be hidden", findText(nodes(), image.name) == null)
                assertTrue("Video filename should be hidden", findText(nodes(), video.name) == null)
                val imageThumb = await("rendered image thumbnail") {
                    control(it, image.name)?.takeIf { node ->
                        val center = node.boundsInWindow.center
                        screenshotPixel(center.x.toInt(), center.y.toInt()) == Color.MAGENTA
                    }
                }
                click(imageThumb)
                await("full-screen image") { nodes ->
                    control(nodes, close)?.takeIf { screenshotPixel() == Color.MAGENTA }
                }
                assertTrue(screenshotPixel(verticalOffset = 0.2f) != Color.MAGENTA)
                doubleTapPreviewCenter()
                await("zoomed image") { screenshotPixel(verticalOffset = 0.2f)?.takeIf { it == Color.MAGENTA } }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                val videoThumb = await("rendered video thumbnail") { nodes ->
                    control(nodes, video.name)?.takeIf { node ->
                        val center = node.boundsInWindow.center
                        screenshotPixel(center.x.toInt(), center.y.toInt())?.let { Color.blue(it) > 200 && Color.red(it) < 40 } == true
                    }
                }
                click(videoThumb)
                await("video playback") { nodes ->
                    control(nodes, pause)?.takeIf {
                        screenshotPixel()?.let { Color.blue(it) > 200 && Color.red(it) < 40 } == true
                    }
                }
                click(await("close video preview") { control(it, close) })
                click(await("remove first attachment") { nodes ->
                    findText(nodes, remove)?.let(::clickable)
                })
                await("only the first attachment removed") { nodes ->
                    findText(nodes, document.name)?.takeIf { control(nodes, image.name) == null && control(nodes, video.name) != null }
                }
            }
        } finally {
            image.delete()
            video.delete()
            document.delete()
        }
    }

    private fun screenshotPixel(x: Int? = null, y: Int? = null, verticalOffset: Float = 0f): Int? {
        val screenshot = instrumentation.uiAutomation.takeScreenshot() ?: return null
        return try {
            val column = x ?: screenshot.width / 2
            val row = y ?: (screenshot.height * (0.5f + verticalOffset)).toInt()
            if (column in 0 until screenshot.width && row in 0 until screenshot.height) screenshot.getPixel(column, row) else null
        } finally {
            screenshot.recycle()
        }
    }

    private fun doubleTapPreviewCenter() {
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val x = screenshot.width / 2f
        val y = screenshot.height / 2f
        screenshot.recycle()
        repeat(2) { index ->
            if (index == 1) SystemClock.sleep(60)
            val down = SystemClock.uptimeMillis()
            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try {
                    instrumentation.sendPointerSync(event)
                } finally {
                    event.recycle()
                }
            }
        }
    }

    private fun findText(nodes: List<SemanticsNode>, label: String): SemanticsNode? =
        nodes.firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true }

    private fun control(nodes: List<SemanticsNode>, label: String): SemanticsNode? =
        nodes.firstOrNull { label in it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
            ?.let(::clickable)

    private fun clickable(node: SemanticsNode): SemanticsNode? =
        generateSequence(node) { it.parent }.firstOrNull { it.config.getOrNull(SemanticsActions.OnClick) != null }

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

    private fun <T : Any> await(expected: String, find: (List<SemanticsNode>) -> T?): T {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            find(nodes())?.let { return it }
            Thread.sleep(25)
        }
        error("Expected $expected")
    }
}
