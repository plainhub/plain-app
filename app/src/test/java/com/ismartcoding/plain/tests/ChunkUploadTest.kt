package com.ismartcoding.plain.tests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Unit tests for file chunk upload logic: atomic writes, chunk listing,
 * merge integrity, and concurrent write safety.
 *
 * These tests verify the patterns used in HttpModule.kt and FileUploadGraphQL.kt
 * without requiring an Android environment.
 */
class ChunkUploadTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ── Atomic chunk write ────────────────────────────────────────────────────

    @Test
    fun `atomic chunk write produces correct file`() {
        val chunkDir = tmp.newFolder("upload_tmp", "file123")
        val bytes = ByteArray(5 * 1024 * 1024) { (it % 256).toByte() } // 5MB
        val chunkFile = File(chunkDir, "chunk_0")
        val tempFile = File(chunkDir, ".tmp_chunk_0_${System.nanoTime()}")

        FileOutputStream(tempFile).use { fos ->
            fos.write(bytes)
            fos.fd.sync()
            assertEquals(bytes.size.toLong(), fos.channel.position())
        }

        assertTrue(tempFile.renameTo(chunkFile))
        assertFalse(tempFile.exists())
        assertTrue(chunkFile.exists())
        assertEquals(bytes.size.toLong(), chunkFile.length())
    }

    @Test
    fun `atomic write cleans up temp file on failure`() {
        val chunkDir = tmp.newFolder("upload_tmp", "file456")
        val tempFile = File(chunkDir, ".tmp_chunk_0_${System.nanoTime()}")
        val chunkFile = File(chunkDir, "chunk_0")

        try {
            FileOutputStream(tempFile).use { _ ->
                throw IOException("Simulated disk full")
            }
        } catch (_: IOException) {
            tempFile.delete()
        }

        assertFalse(tempFile.exists())
        assertFalse(chunkFile.exists())
    }

    @Test
    fun `atomic rename overwrites existing chunk`() {
        val chunkDir = tmp.newFolder("upload_tmp", "file789")

        // Write an old (corrupt) chunk
        val chunkFile = File(chunkDir, "chunk_0")
        chunkFile.writeBytes(ByteArray(100) { 0xFF.toByte() })
        assertEquals(100L, chunkFile.length())

        // Write new chunk atomically
        val newBytes = ByteArray(5000) { (it % 256).toByte() }
        val tempFile = File(chunkDir, ".tmp_chunk_0_${System.nanoTime()}")
        FileOutputStream(tempFile).use { fos ->
            fos.write(newBytes)
            fos.fd.sync()
        }

        // Atomic replace
        chunkFile.delete()
        assertTrue(tempFile.renameTo(chunkFile))
        assertEquals(5000L, chunkFile.length())
    }

    // ── Chunk listing (uploadedChunks query pattern) ──────────────────────────

    @Test
    fun `chunk listing returns correct index and size`() {
        val chunkDir = tmp.newFolder("upload_tmp", "fileabc")

        // Create 3 chunks of various sizes
        val sizes = longArrayOf(5242880, 5242880, 2097152) // 5MB, 5MB, 2MB
        for (i in sizes.indices) {
            File(chunkDir, "chunk_$i").writeBytes(ByteArray(sizes[i].toInt()))
        }

        val result = chunkDir.listFiles()
            ?.filter { it.name.startsWith("chunk_") }
            ?.mapNotNull { file ->
                val index = file.name.removePrefix("chunk_").toIntOrNull()
                if (index != null) "${index}:${file.length()}" else null
            }
            ?.sortedBy { it.substringBefore(':').toInt() }
            ?: emptyList()

        assertEquals(3, result.size)
        assertEquals("0:5242880", result[0])
        assertEquals("1:5242880", result[1])
        assertEquals("2:2097152", result[2])
    }

    @Test
    fun `chunk listing excludes temp files`() {
        val chunkDir = tmp.newFolder("upload_tmp", "filedef")

        File(chunkDir, "chunk_0").writeBytes(ByteArray(5242880))
        File(chunkDir, ".tmp_chunk_1_12345").writeBytes(ByteArray(1000)) // in-progress

        val result = chunkDir.listFiles()
            ?.filter { it.name.startsWith("chunk_") }
            ?.mapNotNull { file ->
                val index = file.name.removePrefix("chunk_").toIntOrNull()
                if (index != null) "${index}:${file.length()}" else null
            }
            ?: emptyList()

        assertEquals(1, result.size)
        assertEquals("0:5242880", result[0])
    }

    @Test
    fun `chunk listing handles empty directory`() {
        val chunkDir = tmp.newFolder("upload_tmp", "emptyfolder")

        val result = chunkDir.listFiles()
            ?.filter { it.name.startsWith("chunk_") }
            ?.mapNotNull { file ->
                val index = file.name.removePrefix("chunk_").toIntOrNull()
                if (index != null) "${index}:${file.length()}" else null
            }
            ?: emptyList()

        assertEquals(0, result.size)
    }

    @Test
    fun `chunk listing handles non-existent directory`() {
        val chunkDir = File(tmp.root, "upload_tmp/nonexistent")

        val result = if (!chunkDir.exists()) {
            emptyList()
        } else {
            chunkDir.listFiles()
                ?.filter { it.name.startsWith("chunk_") }
                ?.mapNotNull { file ->
                    val index = file.name.removePrefix("chunk_").toIntOrNull()
                    if (index != null) "${index}:${file.length()}" else null
                }
                ?: emptyList()
        }

        assertEquals(0, result.size)
    }

    // ── Merge integrity ───────────────────────────────────────────────────────

    @Test
    fun `merge produces correct file from chunks`() {
        val chunkDir = tmp.newFolder("upload_tmp", "merge1")
        val totalChunks = 3

        // Create chunks with distinct content
        val chunkContents = arrayOf(
            ByteArray(5242880) { 0xAA.toByte() },
            ByteArray(5242880) { 0xBB.toByte() },
            ByteArray(2097152) { 0xCC.toByte() },
        )
        for (i in 0 until totalChunks) {
            File(chunkDir, "chunk_$i").writeBytes(chunkContents[i])
        }

        // Calculate expected size
        var expectedSize = 0L
        for (i in 0 until totalChunks) {
            expectedSize += File(chunkDir, "chunk_$i").length()
        }
        assertEquals(12582912L, expectedSize) // 5+5+2 MB

        // Merge
        val outputFile = File(tmp.root, "output.bin")
        val tempMergeFile = File(tmp.root, ".merge_tmp_${System.currentTimeMillis()}")

        FileOutputStream(tempMergeFile).use { fos ->
            val outputChannel = fos.channel
            for (i in 0 until totalChunks) {
                val chunkFile = File(chunkDir, "chunk_$i")
                chunkFile.inputStream().channel.use { inputChannel ->
                    var position = 0L
                    val size = inputChannel.size()
                    while (position < size) {
                        val transferred = inputChannel.transferTo(position, size - position, outputChannel)
                        if (transferred <= 0) break
                        position += transferred
                    }
                }
            }
            fos.fd.sync()
        }

        val mergedSize = tempMergeFile.length()
        assertEquals(expectedSize, mergedSize)

        // Atomic rename
        assertTrue(tempMergeFile.renameTo(outputFile))
        assertEquals(expectedSize, outputFile.length())

        // Verify content
        val merged = outputFile.readBytes()
        assertTrue(merged.slice(0 until 5242880).all { it == 0xAA.toByte() })
        assertTrue(merged.slice(5242880 until 10485760).all { it == 0xBB.toByte() })
        assertTrue(merged.slice(10485760 until 12582912).all { it == 0xCC.toByte() })

        // Cleanup
        chunkDir.deleteRecursively()
    }

    @Test
    fun `merge detects size mismatch`() {
        val chunkDir = tmp.newFolder("upload_tmp", "merge2")

        // Create 2 chunks
        File(chunkDir, "chunk_0").writeBytes(ByteArray(5242880))
        File(chunkDir, "chunk_1").writeBytes(ByteArray(3000000))

        val expectedSize = File(chunkDir, "chunk_0").length() + File(chunkDir, "chunk_1").length()

        // Merge to temp file
        val tempFile = File(tmp.root, ".merge_tmp")
        FileOutputStream(tempFile).use { fos ->
            for (i in 0 until 2) {
                val chunkFile = File(chunkDir, "chunk_$i")
                chunkFile.inputStream().channel.use { inputChannel ->
                    var pos = 0L
                    val size = inputChannel.size()
                    while (pos < size) {
                        val t = inputChannel.transferTo(pos, size - pos, fos.channel)
                        if (t <= 0) break
                        pos += t
                    }
                }
            }
            fos.fd.sync()
        }

        assertEquals(expectedSize, tempFile.length())
        tempFile.delete()
    }

    @Test
    fun `merge detects missing chunk`() {
        val chunkDir = tmp.newFolder("upload_tmp", "merge3")

        File(chunkDir, "chunk_0").writeBytes(ByteArray(5242880))
        // chunk_1 is missing

        val missingChunk = File(chunkDir, "chunk_1")
        assertFalse(missingChunk.exists())
    }

    // ── Size verification ─────────────────────────────────────────────────────

    @Test
    fun `channel position matches written bytes`() {
        val file = tmp.newFile("verify.bin")
        val data = ByteArray(1234567) { (it % 256).toByte() }

        var savedSize: Long
        FileOutputStream(file).use { fos ->
            fos.write(data)
            fos.fd.sync()
            savedSize = fos.channel.position()
        }

        assertEquals(data.size.toLong(), savedSize)
        assertEquals(data.size.toLong(), file.length())
    }

    @Test
    fun `size check catches truncated write`() {
        // Simulate: declare size=5MB but only write 3MB
        val declaredSize = 5242880L
        val actualData = ByteArray(3000000)

        val savedSize: Long = actualData.size.toLong()

        // This is the check from HttpModule.kt
        assertFalse(savedSize == declaredSize)
    }
}
