package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.ClipboardHelper
import com.ismartcoding.plain.platform.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ClipboardRustHttpTest {
    @Test
    fun historyUsesRustAndDeduplicatesWithoutWritingRoom() = runBlocking {
        val marker = "clipboard-rust-${UUID.randomUUID()}"
        val text = "$marker 🌍 100%_ \\ \"quoted\""
        val ids = mutableListOf<String>()
        try {
            val entry = checkNotNull(ClipboardHelper.record(text, source = marker, label = "label-$marker", sensitive = true))
            ids += entry.id
            assertEquals(text, entry.text)
            assertEquals(marker, entry.source)
            assertEquals("label-$marker", entry.label)
            assertTrue(entry.sensitive)
            assertNull(ClipboardHelper.record(text))
            assertEquals(1, ClipboardHelper.count(marker))
            assertEquals(entry, ClipboardHelper.getPage(50, 0, marker).single())
            assertTrue(ClipboardHelper.getPage(50, 1, marker).isEmpty())
            assertNull(AppDatabase.instance.clipboardDao().getById(entry.id))
            assertEquals(0, ClipboardHelper.deleteByIds(emptyList()))
            assertEquals(1, ClipboardHelper.deleteByIds(ids))
            assertEquals(0, ClipboardHelper.count(marker))
        } finally {
            ClipboardHelper.deleteByIds(ids)
        }
    }
}
