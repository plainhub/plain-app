package com.ismartcoding.plain.tests

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.db.DImageEmbedding
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.file.*
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.platform.deleteFileOrDir
import com.ismartcoding.plain.platform.queryFileTaskMedia
import com.ismartcoding.plain.platform.scanFileTaskPaths
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FileRenameDeleteMediaRustHttpTest {
    @Test
    fun ordinaryRenameAndDeleteRebindThenCleanRealImageAndFileMetadata() = runBlocking {
        val prefix = "synthetic-file-media-${UUID.randomUUID()}"
        val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), prefix)
        val target = File(root, "target").apply { check(mkdirs()) }
        val resolver = appContext.contentResolver
        val tags = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        var uri: Uri? = null
        var sourcePath = ""
        var destinationPath = ""
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$prefix.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$prefix/source/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            uri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
            checkNotNull(resolver.openOutputStream(uri)).use { output ->
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                try { bitmap.eraseColor(0xff225588.toInt()); check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) }
                finally { bitmap.recycle() }
            }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            assertEquals(1, resolver.update(uri, values, null, null))
            val oldId = checkNotNull(uri.lastPathSegment).also { ids.add(it) }
            sourcePath = checkNotNull(resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)).use {
                check(it.moveToFirst()); checkNotNull(it.getString(0))
            }
            destinationPath = File(File(sourcePath).parentFile, "renamed.png").path
            val imageTag = TagHelper.addOrUpdate("") { name = prefix; type = DataType.IMAGE.value }.also { tags.add(it) }
            val fileTag = TagHelper.addOrUpdate("") { name = prefix; type = DataType.FILE.value }.also { tags.add(it) }
            TagHelper.editTagRelations(DataType.IMAGE, TagRelationStub(oldId, prefix, 1), listOf(imageTag), emptyList())
            TagHelper.editTagRelations(DataType.FILE, TagRelationStub(sourcePath, prefix, 1), listOf(fileTag), emptyList())
            ImageEmbeddingHelper.insertAll(listOf(DImageEmbedding(oldId, sourcePath, ByteBuffer.allocate(4).putFloat(1f).array())))
            assertNull(renameAndScanFile(sourcePath, "../escape.png"))
            val collision = File(File(sourcePath).parentFile, "existing.png").apply { writeText("existing") }
            assertNull(renameAndScanFile(sourcePath, collision.name))
            assertEquals("existing", collision.readText())
            assertEquals(destinationPath, renameAndScanFile(sourcePath, "renamed.png"))
            assertFalse(File(sourcePath).exists())
            assertTrue(File(destinationPath).isFile)
            val moved = queryFileTaskMedia(listOf(destinationPath)).single()
            assertEquals(DataType.IMAGE, moved.type)
            ids.add(moved.id)
            assertEquals(listOf(imageTag), TagHelper.getTagRelationsByKey(moved.id, DataType.IMAGE).map { it.tagId })
            if (oldId != moved.id) assertTrue(TagHelper.getTagRelationsByKey(oldId, DataType.IMAGE).isEmpty())
            assertTrue(TagHelper.getTagRelationsByKey(sourcePath, DataType.FILE).isEmpty())
            assertEquals(listOf(fileTag), TagHelper.getTagRelationsByKey(destinationPath, DataType.FILE).map { it.tagId })
            assertFalse(ImageEmbeddingHelper.getAllIds().any { it in ids })
            ImageEmbeddingHelper.insertAll(listOf(DImageEmbedding(moved.id, destinationPath, ByteBuffer.allocate(4).putFloat(1f).array())))
            assertTrue(deleteFileOrDir(destinationPath))
            assertFalse(File(destinationPath).exists())
            assertTrue(TagHelper.getTagRelationsByKey(moved.id, DataType.IMAGE).isEmpty())
            assertTrue(TagHelper.getTagRelationsByKey(destinationPath, DataType.FILE).isEmpty())
            assertFalse(ImageEmbeddingHelper.getAllIds().any { it in ids })
            assertFalse(deleteFileOrDir(destinationPath))
        } finally {
            tags.forEach { TagHelper.delete(it) }
            ImageEmbeddingHelper.deleteByIds(ids.toList())
            uri?.let { resolver.delete(it, null, null) }
            ids.forEach { id -> resolver.delete(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id), null, null) }
            root.deleteRecursively()
            scanFileTaskPaths(listOf(sourcePath, destinationPath, File(root, "source").path, target.path, root.path).filter { it.isNotEmpty() })
        }
    }
}
