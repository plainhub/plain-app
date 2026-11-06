package com.ismartcoding.plain.tests

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import com.ismartcoding.plain.ui.app.MainViewModel
import androidx.compose.runtime.snapshotFlow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.sessions.AuthRequest
import com.ismartcoding.plain.features.sessions.RustSessionStore
import com.ismartcoding.plain.features.sessions.RustWebLogin
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.features.sessions.*
import com.ismartcoding.plain.network.*
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Encrypt
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Decrypt
import com.ismartcoding.plain.testing.CryptoFixtures.computeECDHSharedKey
import com.ismartcoding.plain.testing.CryptoFixtures.generateECDHKeyPair
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class SessionMigrationRustHttpTest {
    @Test
    fun encryptedLoginConfirmationSessionUiAndRevocationUseRust() = runBlocking {
        val oldService = Prefs.backgroundServiceEnabled.value
        val oldDesktop = Prefs.remoteAccess.value
        val oldTwoFactor = Prefs.loginRequireApproval.value
        val id = "session-fixture-${UUID.randomUUID()}"
        var pendingId = ""
        val http = createTestHttpClient(TestHttpClientSpec.HttpHost)
        try {
            Prefs.backgroundServiceEnabled.value = true
            Prefs.remoteAccess.value = true
            Prefs.setLoginRequireApproval(true)
            withIO { RustRuntime.start() }
            val client = generateECDHKeyPair()
            val fixturePassword = requireNotNull(InstrumentationRegistry.getArguments().getString("loginPassword")) { "Supply the owned fixture loginPassword argument" }
            val digest = MessageDigest.getInstance("SHA-512").digest(fixturePassword.encodeToByteArray()).joinToString("") { "%02x".format(it) }
            val passwordKey = digest.take(32).encodeToByteArray()
            val request = AuthRequest(digest, "Fixture", "1", "Android", "1", true,
                Base64Lenient.encode(client.publicKeyEncoded))
            val notification = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    snapshotFlow { MainViewModel.pendingLoginRequest.value }.first { it?.clientId == id }!!
                }
            }
            http.webSocket("ws://127.0.0.1:${Prefs.httpPort.value}/auth?cid=$id&auth=1") { socket ->
                socket.sendBinary(chaCha20Encrypt(passwordKey, JsonHelper.jsonEncode(request)))
                val pending = JsonHelper.jsonDecode<JsonObject>(requireNotNull(chaCha20Decrypt(passwordKey,
                    requireNotNull(withTimeout(10_000) { socket.incoming.receive() }.binary))).decodeToString())
                assertEquals("PENDING", pending.getValue("status").jsonPrimitive.content)
                val facts = notification.await()
                pendingId = facts.requestId
                val pendingRequest = facts.request
                assertEquals("", pendingRequest.password)
                RustWebLogin.complete(pendingId)
                val response = JsonHelper.jsonDecode<JsonObject>(requireNotNull(chaCha20Decrypt(passwordKey,
                    requireNotNull(withTimeout(10_000) { socket.incoming.receive() }.binary))).decodeToString())
                assertEquals("COMPLETED", response.getValue("status").jsonPrimitive.content)
                val shared = computeECDHSharedKey(client.privateKeyEncoded,
                    Base64Lenient.decode(response.getValue("ecdhPublicKey").jsonPrimitive.content))
                val session = RustSessionStore.list().single { it.clientId == id }
                assertEquals("Fixture", session.browserName)
                assertFalse(session.isCustom)
                assertArrayEquals(Base64Lenient.decode(requireNotNull(shared)), RustSessionStore.list().firstOrNull { it.clientId == id }?.token?.let(Base64Lenient::decode))
                assertTrue(renameSessionListItemAsync(id, "Renamed fixture"))
                assertEquals("Renamed fixture", fetchSessionsListItemsAsync().single { it.clientId == id }.name)
                assertTrue(runCatching { RustWebLogin.complete(pendingId) }.isFailure)
            }
            deleteSessionListItemAsync(id)
            assertNull(RustSessionStore.list().firstOrNull { it.clientId == id }?.token?.let(Base64Lenient::decode))
        } finally {
            http.close()
            if (pendingId.isNotEmpty()) RustWebLogin.cancel(pendingId)
            RustSessionStore.delete(id)
            Prefs.setLoginRequireApproval(oldTwoFactor)
            Prefs.remoteAccess.value = oldDesktop
            Prefs.backgroundServiceEnabled.value = oldService
        }
    }
}
