package com.ismartcoding.plain.tests

import java.util.UUID
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import com.ismartcoding.plain.features.files.FilesClient

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.Prefs
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class FileBrowseRustHttpTest {
    @Test
    fun sharedBrowserAndPublicQueriesUseRustFiltersPagesCountsAndZipAdapter() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "file-browse-${UUID.randomUUID()}").apply { mkdirs() }
        val permissions = Prefs.apiPermissions.value
        try {
            val dir = File(root, "NeedleDir").apply { mkdirs() }
            File(dir, "NEEDLE 中文.txt").writeText("four")
            File(dir, ".needle-hidden").writeText("hidden")
            File(root, "a.txt").writeText("a")
            File(root, ".hidden").writeText("secret")
            val listed = FilesClient.list(root.path, false, FileSortBy.NAME_ASC)
            assertEquals(listOf("NeedleDir", "a.txt"), listed.map { it.name })
            assertEquals(1, listed.first().childCount)
            assertEquals(3, FilesClient.list(root.path, true, FileSortBy.NAME_ASC).size)
            assertEquals(2, FilesClient.searchName("needle", root.path, false, FileSortBy.NAME_ASC).size)
            assertEquals(3, FilesClient.searchName("needle", root.path, true, FileSortBy.NAME_ASC).size)
            assertEquals(1, FilesClient.search("text:中文 file_size:>=4 file_size:<=4", root.path, FileSortBy.SIZE_DESC).size)
            assertEquals(1, FilesClient.search("parent:\"${dir.path}\" text:中文", File(root, "missing").path, FileSortBy.NAME_ASC).size)
            assertTrue(runCatching { FilesClient.search("parent:/system", root.path, FileSortBy.NAME_ASC) }.isFailure)
            val archive = File(root, "synthetic.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("small.txt")); zip.write("hi".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("large.txt")); zip.write("synthetic".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("folder/child.txt")); zip.write("child".toByteArray()); zip.closeEntry()
            }
            val zipped = FilesClient.search("file_size:>4", "${archive.path}!zip!/", FileSortBy.NAME_ASC)
            assertEquals(listOf("large.txt"), zipped.map { it.name })
            assertEquals("folder", FilesClient.search("", "${archive.path}!zip!/", FileSortBy.NAME_ASC).first().name)
            Prefs.setApiPermissions(permissions + "WRITE_EXTERNAL_STORAGE")
            suspend fun field(selection: String): JsonElement =
                GraphqlApi.execute(GraphqlRequest<JsonObject>("query { $selection }")).values.first()
            assertEquals(true, field("""pathExists(path: "${root.path}")""").jsonPrimitive.boolean)
            assertEquals("DIR", field("""pathKind(path: "${root.path}")""").jsonPrimitive.content)
            assertEquals("FILE", field("""pathKind(path: "${File(root, "a.txt").path}")""").jsonPrimitive.content)
            assertEquals(1L, field("""fileInfo(path: "${File(root, "a.txt").path}") { size }""").jsonObject["size"]!!.jsonPrimitive.long)
            assertEquals(false, field("""pathExists(path: ".")""").jsonPrimitive.boolean)
            assertEquals(false, field("""pathExists(path: "/system")""").jsonPrimitive.boolean)
            assertNull(field("""pathKind(path: "${File(root, "missing").path}")""").jsonPrimitive.contentOrNull)
            assertEquals(3, field("""fileCount(root: "${root.path}", query: "")""").jsonPrimitive.int)
            assertEquals(listOf("a.txt"), field("""files(root: "${root.path}", offset: 1, limit: 1, query: "", sortBy: NAME_ASC) { name }""").jsonArray.map { it.jsonObject["name"]!!.jsonPrimitive.content })
            assertTrue(field("""files(root: "${root.path}", offset: 0, limit: 0, query: "", sortBy: NAME_ASC) { name }""").jsonArray.isEmpty())
            assertEquals(2, field("""fileCount(root: "${root.path}", query: "text:needle")""").jsonPrimitive.int)
            assertEquals(0, field("""fileCount(root: "${File(root, "missing").path}", query: "")""").jsonPrimitive.int)
        } finally {
            Prefs.setApiPermissions(permissions)
            root.deleteRecursively()
        }
    }
}
