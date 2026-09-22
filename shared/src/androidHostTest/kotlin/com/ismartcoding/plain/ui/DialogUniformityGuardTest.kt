package com.ismartcoding.plain.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the dialog uniformity rule (2026-09-21): text-input dialogs MUST be
 * built on ui/base/TextFieldDialog (reference implementation: FileRenameDialog);
 * business code must not hand-roll AlertDialog + text field. A file under ui/
 * outside ui/base that combines AlertDialog( with any *TextField( composable is
 * either legacy (frozen in ALLOWLIST — never add new entries) or a violation.
 * Single-input + confirm/cancel always maps to TextFieldDialog; genuinely
 * multi-field dialogs are the only sanctioned custom AlertDialogs.
 */
class DialogUniformityGuardTest {

    /** Legacy multi-field/special-layout dialogs frozen as-is; do NOT extend. */
    private val allowlist = setOf(
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/components/WebAddressBarEditDialogs.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/components/ColorPickerDialog.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/feeds/AddFeedDialog.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/feeds/EditFeedDialog.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/pomodoro/PomodoroSettingsDialog.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/files/CreateShareDialog.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/ViewTextFileBottomSheet.kt",
    )

    private val uiRoot = "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui"

    /** Resolve the ui source dir from whatever the test working dir is. */
    private fun uiDir(): File {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        repeat(4) {
            val f = File(dir, uiRoot)
            if (f.isDirectory) return f
            dir = dir.parentFile ?: return@repeat
        }
        fail("ui source dir not found: $uiRoot (from ${System.getProperty("user.dir")})")
    }

    @Test
    fun `text input dialogs must build on TextFieldDialog`() {
        val root = uiDir()
        val violations = mutableListOf<String>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            // ui/base hosts the sanctioned dialog builders (TextFieldDialog etc.).
            if (file.independentRelativePath(root).startsWith("base/")) return@forEach
            val text = file.readText()
            if (text.contains("AlertDialog(") && text.contains(Regex("TextField\\("))) {
                val rel = "$uiRoot/${file.independentRelativePath(root)}"
                if (rel !in allowlist) violations.add(rel)
            }
        }
        assertTrue(
            violations.isEmpty(),
            "Hand-rolled text-input AlertDialogs found (use ui/base/TextFieldDialog; " +
                "multi-field customs need an explicit rules review):\n" + violations.joinToString("\n"),
        )
    }

    @Test
    fun `allowlist stays accurate - no stale entries`() {
        val root = uiDir()
        val stale = allowlist.filter { !File(root, it.removePrefix("$uiRoot/")).isFile }
        assertTrue(stale.isEmpty(), "Stale allowlist entries (file deleted):\n${stale.joinToString("\n")}")
    }

    private fun File.independentRelativePath(root: File): String =
        absolutePath.removePrefix(root.absolutePath).removePrefix("/")
}
