package com.ismartcoding.plain.ui.base

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SegmentedButtonsGuardTest {
    @Test
    fun labelsRemainUntruncated() {
        val source = source()
        assertTrue(source.contains("softWrap = false"))
        assertTrue(source.contains("TextOverflow.Visible"))
        assertTrue(!source.contains("TextOverflow.Ellipsis"))
        assertTrue(!source.contains("TextOverflow.Clip"))
        assertTrue(!source.contains(".clip("), "Segment backgrounds must not clip overflowing labels")
    }

    @Test
    fun selectionFeedbackRemainsImmediateAndAccessible() {
        val source = source()
        assertTrue(source.contains("indication = null"))
        assertTrue(source.contains(".selectableGroup()"))
        assertTrue(source.contains("role = Role.RadioButton"))
    }

    private fun source(): String {
        val relative = "src/commonMain/kotlin/com/ismartcoding/plain/ui/base/PSegmentedButtons.kt"
        return listOf(File(relative), File("plain-ui/$relative"))
            .firstOrNull { it.isFile }?.readText()
            ?: error("PSegmentedButtons source not found")
    }
}
