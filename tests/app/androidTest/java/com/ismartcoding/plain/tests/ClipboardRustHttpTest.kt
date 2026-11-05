package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.events.ClipboardReportData
import com.ismartcoding.plain.lib.JsonHelper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.clipboard.ClipboardClient
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.Prefs
import org.junit.Assume.assumeTrue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ClipboardRustHttpTest {
    @Test
    fun nativeClipboardReportsPersistAndDeduplicateInRust() = runBlocking {
        assumeTrue(AccessFeature.CLIPBOARD in Prefs.apiAccessFeatures.value)
        val marker = "clipboard-rust-${UUID.randomUUID()}"
        val text = "$marker 🌍 100%_ \\ \"quoted\""
        val ids = mutableListOf<String>()
        try {
            RustHostReports.clipboard(JsonHelper.jsonEncode(ClipboardReportData(text, sensitive = true)))
            val entry = ClipboardClient.getPage(50, 0, marker).single()
            ids += entry.id
            assertEquals(text, entry.text)
            assertEquals("", entry.source)
            assertEquals("", entry.label)
            assertTrue(entry.sensitive)
            RustHostReports.clipboard(JsonHelper.jsonEncode(ClipboardReportData(text, sensitive = false)))
            assertEquals(1, ClipboardClient.count(marker))
            assertEquals(entry, ClipboardClient.getPage(50, 0, marker).single())
            assertTrue(ClipboardClient.getPage(50, 1, marker).isEmpty())
            assertEquals(0, ClipboardClient.deleteByIds(emptyList()))
            assertEquals(1, ClipboardClient.deleteByIds(ids))
            assertEquals(0, ClipboardClient.count(marker))
        } finally {
            ClipboardClient.deleteByIds(ids)
        }
    }
}
