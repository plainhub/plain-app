package com.ismartcoding.plain.tests

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.features.shares.SharedLinkClient
import com.ismartcoding.plain.features.shares.SharesClient
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.network.*
import com.ismartcoding.plain.testing.CryptoFixtures
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.io.encoding.Base64
import kotlin.time.Instant
import android.system.Os

@RunWith(AndroidJUnit4::class)
class ShareRustHttpTest {
    @Test
    fun sharesResolveFreshRustRootsAndRevokeOldTokens() = runBlocking {
        val marker = "share-rust-${UUID.randomUUID()}"
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, marker).apply { mkdirs() }
        val folder = File(root, "photos").apply { mkdirs() }
        val other = File(root, "other").apply { mkdirs() }
        File(folder, "a.txt").writeText("synthetic")
        File(folder, "empty").mkdir()
        val outside = File(other, "secret.txt").apply { writeText("outside") }
        Os.symlink(outside.absolutePath, File(folder, "escape.txt").absolutePath)
        val service = Prefs.backgroundServiceEnabled.value
        var id: String? = null
        try {
            Prefs.backgroundServiceEnabled.value = true
            withIO { RustRuntime.start() }
            val share = SharesClient.createShare(marker, listOf(folder.absolutePath), true, null)
            id = share.id
            val link = SharedLinkClient.ownLink(share.id, "127.0.0.1")
            val key = Base64.decode(link.token)
            val base = "http://127.0.0.1:${Prefs.httpPort.value}"
            assertTrue(SharesClient.buildLink(share, "127.0.0.1").contains("/s/${share.id}#"))
            createTestHttpClient(TestHttpClientSpec.Local()).use { client ->
                suspend fun guest(path: String = "photos"): Pair<Int, JsonObject?> {
                    val query = buildJsonObject {
                        put("query", "query(\$path: String) { sharedInfo(relativePath: \$path) { entries { relativePath isDir } } }")
                        put("variables", buildJsonObject { put("path", path) })
                    }
                    val body = CryptoFixtures.chaCha20Encrypt(key, "${System.currentTimeMillis()}|${UUID.randomUUID()}|$query")
                    return client.post("$base/guest_graphql", body, "application/octet-stream", mapOf("c-id" to share.id)).use {
                        val status = it.status.value
                        val result = if (status == 200) {
                            val decrypted = CryptoFixtures.chaCha20Decrypt(key, it.bodyAsBytes())
                                ?: throw AssertionError("Guest response decryption failed")
                            Json.parseToJsonElement(decrypted.decodeToString()).jsonObject
                        } else null
                        status to result
                    }
                }
                fun resource(path: String, route: String = "fs"): String {
                    val payload = buildJsonObject { put("sharedId", share.id); put("relativePath", path) }
                    val encrypted = ResourceCrypto.encrypt(payload.toString(), Base64.decode(share.urlToken))
                    return "$base/$route?sid=${share.id}&id=${URLEncoder.encode(encrypted, "UTF-8")}"
                }
                val fileUrl = resource("photos/a.txt")
                client.get(fileUrl).use {
                    assertEquals(200, it.status.value)
                    assertEquals("synthetic", it.bodyAsText())
                }
                val (status, listing) = guest()
                assertEquals(200, status)
                val entries = listing!!.getValue("data").jsonObject.getValue("sharedInfo").jsonObject.getValue("entries").jsonArray
                assertEquals(listOf("photos/a.txt"), entries.filter { !it.jsonObject.getValue("isDir").jsonPrimitive.boolean }
                    .map { it.jsonObject.getValue("relativePath").jsonPrimitive.content })
                client.get(resource("photos", "zip/dir")).use {
                    assertEquals(200, it.status.value)
                    val names = mutableListOf<String>()
                    ZipInputStream(ByteArrayInputStream(it.bodyAsBytes())).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            names.add(entry.name)
                            if (entry.name == "a.txt") assertEquals("synthetic", zip.readBytes().decodeToString())
                        }
                    }
                    assertEquals(listOf("a.txt", "empty/"), names.sorted())
                }
                assertNotNull(guest("photos/../other").second?.get("errors"))
                client.get(resource("photos/escape.txt")).use { assertEquals(403, it.status.value) }
                Prefs.backgroundServiceEnabled.value = false
                try {
                    assertEquals(403, guest().first)
                    client.get(fileUrl).use { assertEquals(403, it.status.value) }
                } catch (error: IOException) {
                    ServerSocket().use { socket ->
                        socket.reuseAddress = true
                        socket.bind(InetSocketAddress(Prefs.httpPort.value))
                        assertTrue("Service shutdown must release the listener: $error", socket.isBound)
                    }
                }
                Prefs.backgroundServiceEnabled.value = true
                withIO { RustRuntime.start() }
                SharesClient.updateShare(share.id, marker, null, listOf(other.absolutePath))
                client.get(fileUrl).use { assertEquals(403, it.status.value) }
                SharesClient.updateShare(share.id, marker, Instant.parse("2000-01-01T00:00:00Z"))
                assertEquals(403, guest("other").first)
                SharesClient.updateShare(share.id, marker, null)
                assertEquals(200, guest("other").first)
                SharesClient.deleteShare(share.id)
                assertEquals(403, guest("other").first)
                assertNull(SharesClient.getShare(share.id))
            }
        } finally {
            id?.let { SharesClient.deleteShare(it) }
            Prefs.backgroundServiceEnabled.value = service
            root.deleteRecursively()
        }
    }
}
