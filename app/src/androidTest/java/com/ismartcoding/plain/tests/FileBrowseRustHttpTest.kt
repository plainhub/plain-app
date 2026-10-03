package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.httpserver.mainschemas.files
import com.ismartcoding.plain.httpserver.mainschemas.fileCount
import com.ismartcoding.plain.httpserver.mainschemas.fileInfo
import com.ismartcoding.plain.httpserver.mainschemas.pathExists
import com.ismartcoding.plain.httpserver.mainschemas.pathKind
import com.ismartcoding.plain.enums.PathKind
import com.ismartcoding.plain.platform.listFilesInDir
import com.ismartcoding.plain.platform.searchFilesInDir
import com.ismartcoding.plain.platform.searchFilesByName
import com.ismartcoding.plain.preferences.SystemPrefs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class FileBrowseRustHttpTest {
    @Test
    fun sharedBrowserAndPublicQueriesUseRustFiltersPagesCountsAndZipAdapter() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "file-browse-${UUID.randomUUID()}").apply { mkdirs() }
        val permissions = SystemPrefs.apiPermissions.value
        try {
            val dir = File(root, "NeedleDir").apply { mkdirs() }
            File(dir, "NEEDLE 中文.txt").writeText("four")
            File(dir, ".needle-hidden").writeText("hidden")
            File(root, "a.txt").writeText("a")
            File(root, ".hidden").writeText("secret")
            val listed = listFilesInDir(root.path, false, FileSortBy.NAME_ASC)
            assertEquals(listOf("NeedleDir", "a.txt"), listed.map { it.name })
            assertEquals(1, listed.first().childCount)
            assertEquals(3, listFilesInDir(root.path, true, FileSortBy.NAME_ASC).size)
            assertEquals(2, searchFilesByName("needle", root.path, false, FileSortBy.NAME_ASC).size)
            assertEquals(3, searchFilesByName("needle", root.path, true, FileSortBy.NAME_ASC).size)
            assertEquals(1, searchFilesInDir("text:中文 file_size:>=4 file_size:<=4", root.path, FileSortBy.SIZE_DESC).size)
            assertEquals(1, searchFilesInDir("parent:\"${dir.path}\" text:中文", File(root, "missing").path, FileSortBy.NAME_ASC).size)
            assertTrue(runCatching { searchFilesInDir("parent:/system", root.path, FileSortBy.NAME_ASC) }.isFailure)
            val archive = File(root, "synthetic.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("small.txt")); zip.write("hi".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("large.txt")); zip.write("synthetic".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("folder/child.txt")); zip.write("child".toByteArray()); zip.closeEntry()
            }
            val zipped = searchFilesInDir("file_size:>4", "${archive.path}!zip!/", FileSortBy.NAME_ASC)
            assertEquals(listOf("large.txt"), zipped.map { it.name })
            assertEquals("folder", searchFilesInDir("", "${archive.path}!zip!/", FileSortBy.NAME_ASC).first().name)
            SystemPrefs.apiPermissions.value = permissions + "WRITE_EXTERNAL_STORAGE"
            assertTrue(pathExists(root.path))
            assertEquals(PathKind.DIR, pathKind(root.path))
            assertEquals(PathKind.FILE, pathKind(File(root, "a.txt").path))
            assertEquals(1L, fileInfo(File(root, "a.txt").path).size)
            assertFalse(pathExists("."))
            assertFalse(pathExists("/system"))
            assertNull(pathKind(File(root, "missing").path))
            assertEquals(3, fileCount(root.path, ""))
            assertEquals(listOf("a.txt"), files(root.path, 1, 1, "", FileSortBy.NAME_ASC).map { it.name })
            assertTrue(files(root.path, 0, 0, "", FileSortBy.NAME_ASC).isEmpty())
            assertEquals(2, fileCount(root.path, "text:needle"))
            assertEquals(0, fileCount(File(root, "missing").path, ""))
        } finally {
            SystemPrefs.apiPermissions.value = permissions
            root.deleteRecursively()
        }
    }
}
