package com.ismartcoding.plain.tests

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import com.ismartcoding.plain.features.files.FilesClient
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.platform.files.scanFileTaskPaths
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.srl_header_last_refresh_just_now

@RunWith(AndroidJUnit4::class)
class FileUiWorkflowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun creates_a_folder_through_the_favorite_directory_and_checks_the_actual_filesystem() = runBlocking {
        check(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        check(InstrumentationRegistry.getArguments().getString("plainUiFixture") == "isolated")
        val root = File(getInternalStoragePath(), "Download/PlainUiFixture/${UUID.randomUUID()}")
        val alias = "UI fixture"
        val folderName = "folder lowercase"
        var created = false
        var favoriteAdded = false
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                try {
                    dismissOnboarding()
                    assertTrue(root.mkdirs())
                    created = true
                    FavoriteFoldersClient.add(getInternalStoragePath(), root.path)
                    favoriteAdded = true
                    FavoriteFoldersClient.rename(root.path, alias)
                    openFiles()
                    click(getString(Res.string.folders))
                    click(alias)
                    click(getString(Res.string.more))
                    click(getString(Res.string.create_folder))
                    val input = awaitNode { it.isEditable }
                    val arguments = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, folderName)
                    }
                    assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
                    click(getString(Res.string.confirm))
                    awaitNode { it.text?.toString() == folderName && !it.isEditable && it.className == "android.widget.TextView" }
                    assertTrue(File(root, folderName).isDirectory)
                    assertTrue(root.listFiles()!!.map { it.name } == listOf(folderName))
                } finally {
                    instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                        try {
                            File(appContext.cacheDir, "ui-file-workflow.png").outputStream().use { output ->
                                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        } finally {
            if (favoriteAdded) FavoriteFoldersClient.remove(root.path)
            if (created) assertTrue(root.deleteRecursively())
        }
    }

    @Test
    fun moves_files_to_trash_with_one_tap_from_details_and_selection() = runBlocking {
        check(appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish")
        check(InstrumentationRegistry.getArguments().getString("plainUiFixture") == "isolated")
        val storage = getInternalStoragePath()
        val root = File(storage, "Download/PlainUiFixture/${UUID.randomUUID()}")
        val alias = "Trash UI fixture"
        val files = listOf("single", "first", "second").map { File(root, "$it-${root.name.take(8)}.txt") }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                dismissOnboarding()
                assertTrue(root.mkdirs())
                files.forEach { it.writeText(it.name) }
                scanFileTaskPaths(files.map { it.path })
                FavoriteFoldersClient.add(storage, root.path)
                FavoriteFoldersClient.rename(root.path, alias)
                openFiles()
                click(getString(Res.string.folders))
                click(alias)

                click(files[0].name, longPress = true)
                trashFromUi(listOf(files[0]))

                click(files[1].name, longPress = true)
                click(getString(Res.string.select))
                click(files[2].name)
                trashFromUi(files.drop(1))

                click(getString(Res.string.folders))
                click(getString(Res.string.trash))
                files.forEach { file -> awaitNode { it.isVisibleToUser && it.text?.toString() == file.name } }

                val trashedPath = FilesClient.trashed().single { it.name == files[0].name }.path
                val folders = getString(Res.string.folders)
                click(files[0].name, longPress = true)
                awaitNode("trash file details") { it.isVisibleToUser && it.hasLabel(trashedPath) }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                awaitNode { it.isVisibleToUser && it.hasLabel(folders) }

                click(files[0].name)
                val wrapContent = getString(Res.string.wrap_content)
                awaitNode("text preview") { it.isVisibleToUser && it.hasLabel(wrapContent) }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                awaitNode { it.isVisibleToUser && it.hasLabel(folders) }
                awaitNode { it.isVisibleToUser && it.hasLabel(files[0].name) }

                pullToRefresh()

                click(files[1].name, longPress = true)
                val select = getString(Res.string.select)
                click(select)
                click(files[2].name)
                val selected = getString(Res.string.x_selected, 2)
                awaitNode { it.isVisibleToUser && it.hasLabel(selected) }
                click(getString(Res.string.restore))
                awaitNode { it.isVisibleToUser && it.hasLabel(folders) }
                assertTrue(files.drop(1).all { it.exists() })
                assertTrue(!files[0].exists())

                click(files[0].name, longPress = true)
                click(select)
                click(getString(Res.string.delete))
                click(getString(Res.string.confirm))
                val noData = getString(Res.string.no_data)
                awaitNode { it.isVisibleToUser && it.hasLabel(noData) }
                assertTrue(FilesClient.trashed().none { entry -> files.any { it.name == entry.name } })

                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                click(folders)
                click(getString(Res.string.trash))
                awaitNode { it.isVisibleToUser && it.hasLabel(noData) }
                pullToRefresh()
            }
        } finally {
            if (root.exists()) {
                FavoriteFoldersClient.remove(root.path)
                val names = files.map { it.name }.toSet()
                val trashedPaths = FilesClient.trashed().filter { it.name in names }.map { it.path }
                if (trashedPaths.isNotEmpty()) FilesClient.restore(trashedPaths)
                assertTrue(root.deleteRecursively())
            }
        }
    }

    private suspend fun dismissOnboarding() {
        val tools = getString(Res.string.tools)
        val skip = getString(Res.string.onboarding_skip)
        val entry = awaitNode { it.isVisibleToUser && (it.hasLabel(tools) || it.hasLabel(skip)) }
        if (entry.hasLabel(skip)) click(skip)
    }

    private suspend fun openFiles() {
        val files = getString(Res.string.files)
        val filesTile = instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
            find(root) { it.isVisibleToUser && it.hasLabel(files) }
        }
        if (filesTile == null) click(getString(Res.string.tools))
        click(files)
    }

    private suspend fun pullToRefresh() {
        val display = instrumentation.targetContext.resources.displayMetrics
        val x = display.widthPixels / 2
        val refreshed = getString(UiRes.string.srl_header_last_refresh_just_now)
        instrumentation.uiAutomation.executeShellCommand(
            "input swipe $x ${display.heightPixels / 4} $x ${display.heightPixels * 3 / 4} 600",
        ).use {
            awaitNode("completed pull to refresh") { it.isVisibleToUser && it.hasLabel(refreshed) }
        }
    }

    private suspend fun trashFromUi(files: List<File>) {
        val trash = getString(Res.string.trash)
        val moveToTrash = getString(Res.string.move_to_trash)
        val label = awaitNode { it.isVisibleToUser && it.text?.toString() in listOf(trash, moveToTrash) }.text.toString()
        click(label)
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (files.any { it.exists() } && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50)
        }
        assertTrue("One tap should move the selected files to Trash", files.none { it.exists() })
        assertEquals(trash, label)
    }

    private fun click(label: String, longPress: Boolean = false) {
        instrumentation.uiAutomation.waitForIdle(300, 5000)
        var control: AccessibilityNodeInfo? = null
        awaitNode("clickable $label") { node ->
            if (!node.isVisibleToUser || !node.isEnabled || !node.hasLabel(label)) return@awaitNode false
            control = generateSequence(node) { it.parent }.take(16).firstOrNull {
                val supportsAction = if (longPress) it.isLongClickable else it.isClickable
                it.isVisibleToUser && it.isEnabled && supportsAction
            }
            control != null
        }
        val action = if (longPress) AccessibilityNodeInfo.ACTION_LONG_CLICK else AccessibilityNodeInfo.ACTION_CLICK
        assertTrue("Click failed: $label", checkNotNull(control).performAction(action))
    }

    private fun AccessibilityNodeInfo.hasLabel(label: String) = text?.toString() == label || contentDescription?.toString() == label

    private fun awaitNode(description: String = "UI control", predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        repeat(300) {
            instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                find(root, predicate)?.let { return it }
            }
            SystemClock.sleep(100)
        }
        error("Expected $description did not become available")
    }

    private fun find(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> find(child, predicate)?.let { return it } }
        }
        return null
    }
}
