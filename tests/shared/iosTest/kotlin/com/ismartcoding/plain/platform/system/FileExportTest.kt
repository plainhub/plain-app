@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.files.copyFileToUri
import com.ismartcoding.plain.platform.files.readTextFile
import com.ismartcoding.plain.platform.files.writeBytesToPath

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.rust.RustStartupConfig
import com.ismartcoding.plain.rust.RustStartupResult
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rustprefs.plain_core_stop
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.symlink

class FileExportTest {
    @Test
    fun pickedTextFileUrlsReadTheirDecodedLocalPathWithoutChangingTheSource() = runTest {
        val root = NSTemporaryDirectory() + NSUUID().UUIDString
        val manager = NSFileManager.defaultManager
        assertTrue(manager.createDirectoryAtPath(root, true, null, null))
        try {
            val path = "$root/feed + \u6d4b\u8bd5.opml"
            val content = "<opml><title>\u6d4b\u8bd5</title></opml>"
            val bytes = content.encodeToByteArray()
            assertTrue(writeBytesToPath(path, bytes))
            val uri = NSURL.fileURLWithPath(path).absoluteString!!
            assertTrue(uri.contains("%20"))
            assertEquals(content, readTextFile(uri))
            assertEquals(content, readTextFile(path))
            assertEquals("", readTextFile("https://example.invalid/feed.opml"))
            assertEquals("", readTextFile(NSURL.fileURLWithPath("$root/missing.opml").absoluteString!!))
            assertContentEquals(bytes, manager.contentsAtPath(path)!!.toByteArray())
        } finally {
            manager.removeItemAtPath(root, null)
        }
    }

    @Test
    fun selectedFileUrlsExportBytesAndAliasesCannotDeleteTheirSource() = runTest {
        val root = NSTemporaryDirectory() + NSUUID().UUIDString
        val manager = NSFileManager.defaultManager
        assertTrue(manager.createDirectoryAtPath(root, true, null, null))
        try {
            val dataDir = "$root/data"
            val config = RustStartupConfig(
                debug = true,
                cacheDir = "$root/cache",
                name = "File Export Fixture",
                deviceType = "PHONE",
                buildChannel = "GITHUB",
                downloadsDir = "$dataDir/downloads",
                wifiAwareSupported = false,
                recentFilesCatalog = false,
                systemMediaCatalog = false,
                supportedAbis = emptyList(),
            )
            val startup = JsonHelper.jsonDecode<RustStartupResult>(
                RustNativeBindings.startCore(dataDir, generateChaCha20Key(), JsonHelper.jsonEncode(config)),
            )
            assertEquals("", startup.errorCode, startup.error)
            val source = "$dataDir/source.zip"
            val target = "$dataDir/export.zip"
            val alias = "$dataDir/source-alias.zip"
            val bytes = byteArrayOf(0, 1, 127, -1)
            assertTrue(writeBytesToPath(source, bytes))
            assertTrue(copyFileToUri(source, NSURL.fileURLWithPath(target).absoluteString!!))
            assertContentEquals(bytes, manager.contentsAtPath(target)!!.toByteArray())
            assertEquals(0, symlink(source, alias))
            assertFalse(copyFileToUri(source, NSURL.fileURLWithPath(alias).absoluteString!!))
            assertContentEquals(bytes, manager.contentsAtPath(source)!!.toByteArray())
            assertFalse(copyFileToUri(source, "https://example.invalid/export.zip"))
        } finally {
            plain_core_stop()
            manager.removeItemAtPath(root, null)
        }
    }
}
