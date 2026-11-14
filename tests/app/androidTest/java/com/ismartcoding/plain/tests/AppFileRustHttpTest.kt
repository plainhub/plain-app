package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.ui.extensions.getFinalPath
import com.ismartcoding.plain.features.files.stored.AppFileStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppFileRustHttpTest {
    @Test
    fun attachmentsShareRustReferencesAndCanonicalFiles() = runBlocking {
        val marker = "file-rust-${UUID.randomUUID()}"
        val data = marker.toByteArray()
        val ids = mutableListOf<String>()
        val source = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, marker)
        try {
            val first = AppFileFixtures.importBytes(data,"text/plain")
            ids += first.id
            val canonical = File(AppFileStore.toFidUri(first).getFinalPath())
            assertArrayEquals(data,canonical.readBytes())
            source.writeBytes(data)
            val second = AppFileStore.uploadFile(source.absolutePath,"original.properties","text/plain")
            ids += second.id
            assertEquals(first.id,second.id)
            assertEquals(first.realPath,second.realPath)
            assertEquals(first.createdAt,second.createdAt)
            assertTrue(source.exists())
            assertEquals(first.id,AppFileStore.page(0,20,"text:${first.id}").single().appFile.id)
            assertEquals(1,AppFileStore.count("text:${first.id}"))
            AppFileStore.release(ids.removeAt(0))
            assertTrue(canonical.exists())
            assertTrue(AppFileFixtures.exists(first.id))
            AppFileStore.release(ids.removeAt(0))
            assertFalse(canonical.exists())
            assertFalse(AppFileFixtures.exists(first.id))
            AppFileStore.release(first.id)
        } finally {
            ids.forEach { AppFileStore.release(it) }
            source.delete()
        }
    }
}
