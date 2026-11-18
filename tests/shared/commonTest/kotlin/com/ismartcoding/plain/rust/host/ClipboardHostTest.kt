package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.rust.host.provider.SystemAppHost
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardHostTest {
    @Test
    fun systemClipboardWritePreservesExplicitLabelAndSensitivity() = runTest {
        val writes = mutableListOf<Triple<String, String, Boolean>>()
        for (label in listOf("fixture label", "")) {
            SystemAppHost.handle("systemSetClipboard", buildJsonObject {
                put("text", "synthetic clipboard")
                put("label", label)
                put("sensitive", true)
            }) { clipLabel, text, sensitive -> writes += Triple(clipLabel, text, sensitive) }
        }
        assertEquals(listOf(
            Triple("fixture label", "synthetic clipboard", true),
            Triple("", "synthetic clipboard", true),
        ), writes)
    }

    @Test
    fun textOnlySystemClipboardWriteKeepsExistingDefaults() = runTest {
        var write: Triple<String, String, Boolean>? = null
        SystemAppHost.handle("systemSetClipboard", buildJsonObject {
            put("text", "synthetic clipboard")
        }) { label, text, sensitive -> write = Triple(label, text, sensitive) }
        assertEquals(Triple("plain", "synthetic clipboard", false), write)
    }

    @Test
    fun settings_changes_and_host_shutdown_own_exactly_one_observation() = runTest {
        val enabled = MutableStateFlow(false)
        var starts = 0
        var stops = 0
        var active = 0
        val host = launch {
            observeClipboardWhenEnabled(enabled) {
                starts++
                active++
                assertEquals(1, active)
                try { awaitCancellation() }
                finally { active--; stops++ }
            }
        }
        runCurrent()
        assertEquals(0, starts)
        enabled.value = true
        runCurrent()
        enabled.value = true
        runCurrent()
        assertEquals(1, starts)
        enabled.value = false
        runCurrent()
        assertEquals(1, stops)
        enabled.value = true
        runCurrent()
        assertEquals(2, starts)
        host.cancelAndJoin()
        assertEquals(2, stops)
        assertEquals(0, active)
        enabled.value = true
        runCurrent()
        assertEquals(2, starts)
    }
}
