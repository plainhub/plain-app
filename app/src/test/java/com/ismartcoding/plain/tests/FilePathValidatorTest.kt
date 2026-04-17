package com.ismartcoding.plain.tests

import com.ismartcoding.plain.helpers.FilePathValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for [FilePathValidator] verifying that batch delete cannot be used
 * to accidentally or maliciously wipe critical paths on the Android filesystem.
 */
class FilePathValidatorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ── Blank / non-absolute ──────────────────────────────────────────────────

    @Test fun `blank path is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete(""))
        assertFalse(FilePathValidator.isSafeToDelete("   "))
    }

    @Test fun `relative path is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("relative/path/file.txt"))
    }

    // ── Root / single-component paths ─────────────────────────────────────────

    @Test fun `filesystem root is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/"))
    }

    @Test fun `single component path is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/sdcard"))
        assertFalse(FilePathValidator.isSafeToDelete("/storage"))
        assertFalse(FilePathValidator.isSafeToDelete("/data"))
    }

    // ── Forbidden system prefixes ─────────────────────────────────────────────

    @Test fun `system directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/system"))
        assertFalse(FilePathValidator.isSafeToDelete("/system/bin"))
        assertFalse(FilePathValidator.isSafeToDelete("/system/lib64/libc.so"))
    }

    @Test fun `proc directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/proc"))
        assertFalse(FilePathValidator.isSafeToDelete("/proc/1/maps"))
    }

    @Test fun `sys directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/sys"))
        assertFalse(FilePathValidator.isSafeToDelete("/sys/block"))
    }

    @Test fun `dev directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/dev"))
        assertFalse(FilePathValidator.isSafeToDelete("/dev/null"))
    }

    @Test fun `data app directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/data/app"))
        assertFalse(FilePathValidator.isSafeToDelete("/data/app/com.example-1"))
    }

    @Test fun `data data directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/data/data"))
        assertFalse(FilePathValidator.isSafeToDelete("/data/data/com.ismartcoding.plain"))
    }

    @Test fun `vendor directory is rejected`() {
        assertFalse(FilePathValidator.isSafeToDelete("/vendor"))
        assertFalse(FilePathValidator.isSafeToDelete("/vendor/lib/libtest.so"))
    }

    // ── Path traversal ────────────────────────────────────────────────────────

    @Test fun `path traversal into system is rejected`() {
        // On a real filesystem these canonicalize to /system/... or stay put;
        // either way the validator must reject them.
        assertFalse(FilePathValidator.isSafeToDelete("/sdcard/../system/bin"))
        assertFalse(FilePathValidator.isSafeToDelete("/sdcard/../../system"))
    }

    // ── allowedRoots enforcement ──────────────────────────────────────────────

    @Test fun `path outside allowed roots is rejected`() {
        val allowedRoots = listOf("/sdcard", "/storage/emulated/0")
        // /tmp is not under any allowed root
        val outsidePath = "/tmp/somefile.txt"
        assertFalse(FilePathValidator.isSafeToDelete(outsidePath, allowedRoots))
    }

    @Test fun `path inside allowed root is accepted`() {
        val root = tmp.newFolder("userfiles").absolutePath
        val file = tmp.newFile("userfiles/photo.jpg").absolutePath
        assertTrue(FilePathValidator.isSafeToDelete(file, listOf(root)))
    }

    @Test fun `path with traversal escaping allowed root is rejected`() {
        val root = tmp.newFolder("userfiles2").absolutePath
        val escape = "$root/../../etc/passwd"
        assertFalse(FilePathValidator.isSafeToDelete(escape, listOf(root)))
    }

    // ── Valid paths (no allowed-roots restriction) ────────────────────────────

    @Test fun `valid two-component path under tmp is accepted`() {
        val dir = tmp.newFolder("mydir").absolutePath
        val file = tmp.newFile("mydir/notes.txt").absolutePath
        assertTrue(FilePathValidator.isSafeToDelete(file))
        assertTrue(FilePathValidator.isSafeToDelete(dir))
    }

    @Test fun `valid deeply nested path is accepted`() {
        val file = tmp.newFile("deep.txt").absolutePath
        assertTrue(FilePathValidator.isSafeToDelete(file))
    }

    // ── requireAllSafe ────────────────────────────────────────────────────────

    @Test fun `requireAllSafe throws on any bad path`() {
        val good = tmp.newFile("ok.txt").absolutePath
        try {
            FilePathValidator.requireAllSafe(listOf(good, "/system/bin/su"))
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("/system/bin/su"))
        }
    }

    @Test fun `requireAllSafe passes when all paths are safe`() {
        val f1 = tmp.newFile("a.txt").absolutePath
        val f2 = tmp.newFile("b.txt").absolutePath
        // Should not throw
        FilePathValidator.requireAllSafe(listOf(f1, f2))
    }

    @Test fun `requireAllSafe rejects empty path in list`() {
        val good = tmp.newFile("c.txt").absolutePath
        try {
            FilePathValidator.requireAllSafe(listOf(good, ""))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { /* expected */ }
    }
}
