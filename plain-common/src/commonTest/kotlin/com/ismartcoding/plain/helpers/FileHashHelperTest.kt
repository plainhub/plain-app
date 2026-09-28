package com.ismartcoding.plain.helpers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileHashHelperTest {

    @Test fun `strongHash returns lowercase 64-char sha256`() {
        val hash = FileHashHelper.strongHash("abc".encodeToByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash,
        )
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isLowerCase() || it.isDigit() })
    }

    @Test fun `strongHash is deterministic for identical bytes`() {
        val data = "hello world".encodeToByteArray()
        assertEquals(FileHashHelper.strongHash(data), FileHashHelper.strongHash(data))
    }

    @Test fun `strongHash differs when bytes differ`() {
        assertTrue(
            FileHashHelper.strongHash("a".encodeToByteArray()) !=
                FileHashHelper.strongHash("b".encodeToByteArray()),
        )
    }

    @Test fun `weakHash equals strongHash for data within edge window`() {
        // 2 * EDGE_BYTES = 8192; small buffers are hashed whole.
        val data = "small".encodeToByteArray()
        assertEquals(FileHashHelper.strongHash(data), FileHashHelper.weakHash(data))
    }

    @Test fun `weakHash of empty buffer is sha256 of empty`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            FileHashHelper.weakHash(byteArrayOf()),
        )
    }

    @Test fun `weakHash ignores middle bytes for large data`() {
        val size = 2 * FileHashEdgeBytes + 1000
        val data = ByteArray(size) { (it % 251).toByte() }
        val mutated = data.copyOf().also { it[size / 2] = 0x7F.toByte() } // hmm middle index is excluded

        val weakA = FileHashHelper.weakHash(data)
        val weakB = FileHashHelper.weakHash(mutated)
        assertEquals(weakA, weakB, "middle bytes must not affect the edge-based weak hash")

        assertTrue(
            FileHashHelper.strongHash(data) != FileHashHelper.strongHash(mutated),
            "strong hash must still detect the middle change",
        )
    }

    @Test fun `weakHash reflects first and last edge bytes for large data`() {
        val size = 3 * FileHashEdgeBytes
        val data = ByteArray(size) { (it % 251).toByte() }
        val edgeBuf = data.copyOfRange(0, FileHashEdgeBytes) +
            data.copyOfRange(size - FileHashEdgeBytes, size)
        assertEquals(FileHashHelper.weakHash(edgeBuf), FileHashHelper.weakHash(data))
    }

    @Test fun `weakHash stays 64 hex chars for large data`() {
        val hash = FileHashHelper.weakHash(ByteArray(3 * FileHashEdgeBytes))
        assertEquals(64, hash.length)
    }

    companion object {
        private const val FileHashEdgeBytes = 4 * 1024
    }
}