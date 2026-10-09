package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.extensions.getFinalPath
import com.ismartcoding.plain.helpers.AppFileStore
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
            val first = AppFileStore.importBytes(data,"text/plain")
            ids += first.id
            assertEquals(1,first.refCount)
            val canonical = File(AppFileStore.toFidUri(first).getFinalPath())
            assertArrayEquals(data,canonical.readBytes())
            source.writeBytes(data)
            val second = AppFileStore.importFile(source.absolutePath,"original.properties","text/plain",deleteSrc=true)
            ids += second.id
            assertEquals(first.id,second.id)
            assertEquals(first.realPath,second.realPath)
            assertEquals(first.createdAt,second.createdAt)
            assertEquals(2,second.refCount)
            assertFalse(source.exists())
            assertEquals(first.id,AppFileStore.page(0,20,"text:${first.id}").single().appFile.id)
            assertEquals(1,AppFileStore.count("text:${first.id}"))
            AppFileStore.release(ids.removeAt(0))
            assertTrue(canonical.exists())
            assertEquals(1,checkNotNull(AppFileStore.getById(first.id)).refCount)
            AppFileStore.release(ids.removeAt(0))
            assertFalse(canonical.exists())
            assertNull(AppFileStore.getById(first.id))
            AppFileStore.release(first.id)
        } finally {
            ids.forEach { AppFileStore.release(it) }
            source.delete()
        }
    }
}
