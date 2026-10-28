package com.ismartcoding.plain.tests

import kotlinx.serialization.json.JsonObject
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.features.files.FilesClient

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.platform.files.scanFileTaskPaths
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileWritesRustHttpTest {
    @Test
    fun sharedAndPublicFileWritesUseRustAndPreserveExistingContent() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "file-writes-${UUID.randomUUID()}")
        val external = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "file-writes-${UUID.randomUUID()}")
        val originalPermissions = Prefs.apiPermissions.value
        try {
            val dir = FilesClient.createDirectory(File(root, "nested/child").path)
            assertTrue(dir.isDir)
            assertTrue(File(dir.path).isDirectory)
            val file = File(dir.path, "中文.txt")
            val empty = FilesClient.createFile(file.path)
            assertFalse(empty.isDir)
            assertEquals(0L, empty.size)
            val content = "synthetic 中文\n"
            val written = FilesClient.writeText(file.path, content, true)
            assertEquals(content.toByteArray().size.toLong(), written.size)
            assertEquals(content, file.readText())
            val uriFile = File(dir.path, "space + name.txt")
            FilesClient.writeText("file://${dir.path}/space%20+%20name.txt", content, false)
            assertEquals(content, uriFile.readText())
            FilesClient.createFile(file.path)
            assertEquals(content, file.readText())
            assertTrue(runCatching { FilesClient.writeText(file.path, "rejected", false) }.isFailure)
            assertEquals(content, file.readText())
            assertTrue(runCatching { FilesClient.createFile(File(root, "missing/never.txt").path) }.isFailure)
            assertFalse(File(root, "missing/never.txt").exists())
            assertTrue(runCatching { FilesClient.createDirectory("/system/plain-synthetic-${UUID.randomUUID()}") }.isFailure)
            Prefs.setApiPermissions(originalPermissions + "WRITE_EXTERNAL_STORAGE")
            val publicDir = File(root, "public")
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + """createDir(path: "${publicDir.path}")""" + " }"))
            assertTrue(publicDir.isDirectory)
            val publicFile = File(publicDir, "public.txt")
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + """writeTextFile(path: "${publicFile.path}", content: "$content", overwrite: false)""" + " }"))
            assertEquals(content, publicFile.readText())
            assertTrue(runCatching { GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + """writeTextFile(path: "${publicFile.path}", content: "rejected", overwrite: false)""" + " }")) }.isFailure)
            assertEquals(content, publicFile.readText())
            FilesClient.createDirectory(external.path)
            val externalFile = File(external, "synthetic.txt")
            FilesClient.writeText(externalFile.path, content, false)
            assertEquals(content, externalFile.readText())
        } finally {
            Prefs.setApiPermissions(originalPermissions)
            root.deleteRecursively()
            external.deleteRecursively()
            scanFileTaskPaths(listOf(File(external, "synthetic.txt").path, external.path))
        }
    }
}
