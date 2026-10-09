package com.ismartcoding.uicheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinUiCheckTest {
    private fun inspect(source: String, library: Boolean = false) = KotlinUiCheck().use { it.inspect("Example.kt", source, library).first }

    @Test
    fun aliasesQualifiedCallsAndWildcardImportsAreChecked() {
        val findings = inspect("""
            import androidx.compose.material3.Button as Action
            import androidx.compose.material3.*
            fun Content() {
                Action(onClick = {}) {}
                androidx.compose.material3.TextField(
                    value = "", onValueChange = {}
                )
                Scaffold {}
                Switch(checked = true, onCheckedChange = {})
            }
        """.trimIndent())
        assertEquals(3, findings.size)
        assertTrue(findings.all { it.key.contains("|Content|") })
        assertEquals(listOf("PFilledButton", "PTextField", "PScaffold"), findings.map { it.replacement })
    }

    @Test
    fun stringsCommentsAndLocalDeclarationsAreNotCalls() {
        assertTrue(inspect("""
            import androidx.compose.material3.Button
            // Button(onClick = {})
            val text = "androidx.compose.material3.Scaffold()"
            fun Button() {}
            fun Content() { Button() }
        """.trimIndent()).isEmpty())
    }

    @Test
    fun publicLayoutContractsExcludePrivateHelpersAndStateFactories() {
        val findings = inspect("""
            import androidx.compose.runtime.Composable as Ui
            @Ui fun Bad(onClick: () -> Unit = {}) {}
            @Ui fun Good(modifier: Modifier = Modifier) {}
            @Ui private fun Helper() {}
            @Ui fun rememberExampleState() = State()
        """.trimIndent(), library = true)
        assertEquals(setOf("missing-modifier", "empty-action", "multiple-components"), findings.map { it.key.substringAfterLast('|') }.toSet())
    }

    @Test
    fun staticallyInvalidComponentCombinationsAreRejected() {
        val findings = inspect("""
            import com.ismartcoding.plain.ui.base.*
            fun Content() {
                PSheetPrimaryActionsCard {
                    PSheetPrimaryAction(icon, "1") {}
                    PSheetPrimaryAction(icon, "2") {}
                    PSheetPrimaryAction(icon, "3") {}
                    PSheetPrimaryTrashAction("4") {}
                    PSheetPrimaryDeleteAction("5") {}
                }
                PListItem(title = "Title", titleTrailing = {}, titleSuffix = {})
                PInputChip(text = "Chip", onClick = null)
            }
        """.trimIndent())
        assertEquals(setOf("sheet-overflow", "conflicting-title-slots", "chip-without-action"), findings.map { it.key.substringAfterLast('|') }.toSet())
    }

    @Test
    fun dynamicActionsRemainRuntimeValidatedAndValidSlotsPass() {
        assertTrue(inspect("""
            import com.ismartcoding.plain.ui.base.*
            fun Content() {
                PSheetPrimaryActionsCard { actions.forEach { PSheetPrimaryAction(it.icon, it.text) {} } }
                PListItem(title = "Title", titleSuffix = {})
                PInputChip(text = "Chip", onClick = null, onClose = {})
            }
        """.trimIndent()).isEmpty())
    }
}
