package com.ismartcoding.plain.tests

import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.models.AppFile
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

object AppFileFixtures {
    suspend fun importBytes(bytes: ByteArray, mimeType: String): AppFile {
        val source = File.createTempFile("attachment-fixture-", ".tmp", InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)
        try {
            source.writeBytes(bytes)
            return AppFileStore.uploadFile(source.absolutePath, mimeType = mimeType)
        } finally {
            source.delete()
        }
    }

    suspend fun exists(id: String): Boolean = AppFileStore.page(0, 1, "text:$id").isNotEmpty()
}
