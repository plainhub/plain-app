package com.ismartcoding.plain.ui.base

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PListItemGuardTest {
    @Test
    fun keyWithValueStaysOnOneLineWithoutEllipsis() {
        val source = source()
        val keyRow = source.substringAfter("else -> if (value != null) {").substringBefore("\n                } else {")
        assertTrue(keyRow.contains("maxLines = 1"), "Key must stay on one line when a value is shown")
        assertTrue(keyRow.contains("softWrap = false"), "Key must not break mid-word")
        assertTrue(keyRow.contains("TextOverflow.Clip"), "Key must not be ellipsized")
        assertTrue(!keyRow.contains("TextOverflow.Ellipsis"), "Key must not be ellipsized")
        assertTrue(keyRow.contains("Modifier.weight(1f)"), "Key must take only the space the value leaves")
    }

    @Test
    fun valueStaysWithinHalfTheRowAndWraps() {
        val source = source()
        val trailing = source.substringAfter("trailingContent = if (value != null").substringBefore("if (showMore)")
        assertTrue(trailing.contains("Modifier.fillMaxWidth(0.5f)"), "Value column must not exceed half the row")
        assertTrue(trailing.contains("Modifier.weight(1f)"), "Value must wrap inside its own column")
        assertTrue(!trailing.contains("maxLines"), "Value must be allowed to wrap onto multiple lines")
    }

    private fun source(): String {
        val relative = "src/commonMain/kotlin/com/ismartcoding/plain/ui/base/PListItem.kt"
        return listOf(File(relative), File("plain-ui/$relative"))
            .firstOrNull { it.isFile }?.readText()
            ?: error("PListItem source not found")
    }
}