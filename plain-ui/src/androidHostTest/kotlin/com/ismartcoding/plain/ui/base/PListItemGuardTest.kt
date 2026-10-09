package com.ismartcoding.plain.ui.base

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PListItemGuardTest {
    @Test
    fun keyWithValueNeverBreaksMidWord() {
        val source = source()
        val plainTitle = source.substringAfter("else -> Text(").substringBefore("\n                }")
        assertTrue(
            plainTitle.contains("maxLines = if (value != null) 1 else Int.MAX_VALUE"),
            "Plain title must stay on one line when a value is shown: $plainTitle",
        )
        assertTrue(plainTitle.contains("softWrap = value == null"), "Title must not soft-wrap when a value is shown")
        assertTrue(plainTitle.contains("TextOverflow.Ellipsis"), "Title must ellipsize instead of breaking mid-word")
    }

    private fun source(): String {
        val relative = "src/commonMain/kotlin/com/ismartcoding/plain/ui/base/PListItem.kt"
        return listOf(File(relative), File("plain-ui/$relative"))
            .firstOrNull { it.isFile }?.readText()
            ?: error("PListItem source not found")
    }
}