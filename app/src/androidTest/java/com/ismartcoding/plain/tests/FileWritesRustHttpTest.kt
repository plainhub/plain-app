package com.ismartcoding.plain.tests

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.platform.createDirectory
import com.ismartcoding.plain.platform.createFile
import com.ismartcoding.plain.platform.writeFileText
import com.ismartcoding.plain.platform.scanFileTaskPaths
import com.ismartcoding.plain.preferences.RustSystemState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FileWritesRustHttpTest {
    @Test
    fun sharedAndPublicFileWritesUseRustAndPreserveExistingContent() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "file-writes-${UUID.randomUUID()}")
        val external = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "file-writes-${UUID.randomUUID()}")
        val originalPermissions = RustSystemState.state.value.apiPermissions
        try {
            val dir = createDirectory(File(root, "nested/child").path)
            assertTrue(dir.isDir)
            assertTrue(File(dir.path).isDirectory)
            val file = File(dir.path, "中文.txt")
            val empty = createFile(file.path)
            assertFalse(empty.isDir)
            assertEquals(0L, empty.size)
            val content = "synthetic 中文\n"
            val written = writeFileText(file.path, content, true)
            assertEquals(content.toByteArray().size.toLong(), written.size)
            assertEquals(content, file.readText())
            val uriFile = File(dir.path, "space + name.txt")
            writeFileText("file://${dir.path}/space%20+%20name.txt", content, false)
            assertEquals(content, uriFile.readText())
            createFile(file.path)
            assertEquals(content, file.readText())
            assertTrue(runCatching { writeFileText(file.path, "rejected", false) }.isFailure)
            assertEquals(content, file.readText())
            assertTrue(runCatching { createFile(File(root, "missing/never.txt").path) }.isFailure)
            assertFalse(File(root, "missing/never.txt").exists())
            assertTrue(runCatching { createDirectory("/system/plain-synthetic-${UUID.randomUUID()}") }.isFailure)
            RustSystemState.setApiPermissions(originalPermissions + "WRITE_EXTERNAL_STORAGE")
            val publicDir = File(root, "public")
            RustContentApi.mutate("""createDir(path: "${publicDir.path}")""")
            assertTrue(publicDir.isDirectory)
            val publicFile = File(publicDir, "public.txt")
            RustContentApi.mutate("""writeTextFile(path: "${publicFile.path}", content: "$content", overwrite: false)""")
            assertEquals(content, publicFile.readText())
            assertTrue(runCatching { RustContentApi.mutate("""writeTextFile(path: "${publicFile.path}", content: "rejected", overwrite: false)""") }.isFailure)
            assertEquals(content, publicFile.readText())
            createDirectory(external.path)
            val externalFile = File(external, "synthetic.txt")
            writeFileText(externalFile.path, content, false)
            assertEquals(content, externalFile.readText())
        } finally {
            RustSystemState.setApiPermissions(originalPermissions)
            root.deleteRecursively()
            external.deleteRecursively()
            scanFileTaskPaths(listOf(File(external, "synthetic.txt").path, external.path))
        }
    }
}
