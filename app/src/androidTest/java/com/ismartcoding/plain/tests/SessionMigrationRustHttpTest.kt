package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.features.session.RustSessionStore
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.features.session.RustWebLogin
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionMigrationRustHttpTest {
    @Test
    fun encryptedLoginConfirmationSessionUiAndRevocationUseRust() = runBlocking {
        val oldService = UserPrefs.service.value
        val oldDesktop = UserPrefs.desktopAccess.value
        val oldTwoFactor = RustSystemState.state.value.authTwoFactor
        val id = "session-fixture-${UUID.randomUUID()}"
        var pendingId = ""
        try {
            UserPrefs.service.set(true)
            UserPrefs.desktopAccess.set(true)
            RustSystemState.setTwoFactor(true)
            val client = generateECDHKeyPair()
            val digest = sha512(RustSystemState.state.value.password.encodeToByteArray())
            val passwordKey = digest.take(32).encodeToByteArray()
            val request = buildJsonObject {
                put("password", digest)
                put("browserName", "Fixture")
                put("browserVersion", "1")
                put("osName", "Android")
                put("osVersion", "1")
                put("isMobile", true)
                put("ecdhPublicKey", Base64Lenient.encode(client.publicKeyEncoded))
            }
            val pending = RustContentApi.postJsonOrThrow("system/ws-login", buildJsonObject {
                put("action", "issue")
                put("clientId", id)
                put("clientIp", "127.0.0.1")
                put("frame", Base64Lenient.encode(chaCha20Encrypt(passwordKey, request.toString())))
            })
            assertEquals("PENDING", pending.getValue("status").jsonPrimitive.content)
            pendingId = pending.getValue("requestId").jsonPrimitive.content
            val complete = RustContentApi.postJsonOrThrow("system/ws-login", buildJsonObject {
                put("action", "complete"); put("requestId", pendingId)
            })
            val response = Json.parseToJsonElement(requireNotNull(chaCha20Decrypt(passwordKey,
                Base64Lenient.decode(complete.getValue("frame").jsonPrimitive.content))).decodeToString()).jsonObject
            assertEquals("COMPLETED", response.getValue("status").jsonPrimitive.content)
            val shared = computeECDHSharedKey(client.privateKeyEncoded,
                Base64Lenient.decode(response.getValue("ecdhPublicKey").jsonPrimitive.content))
            assertEquals(shared, complete.getValue("token").jsonPrimitive.content)
            val session = RustSessionStore.list().single { it.clientId == id }
            assertEquals("Fixture", session.browserName)
            assertFalse(session.isCustom)
            assertArrayEquals(Base64Lenient.decode(requireNotNull(shared)), RustSessionStore.key(id))
            assertTrue(renameSessionListItemAsync(id, "Renamed fixture"))
            assertEquals("Renamed fixture", fetchSessionsListItemsAsync().single { it.clientId == id }.name)
            assertTrue(RustContentApi.postJson("system/ws-login", buildJsonObject {
                put("action", "complete"); put("requestId", pendingId)
            }).isFailure)
            deleteSessionListItemAsync(id)
            assertNull(RustSessionStore.key(id))
        } finally {
            if (pendingId.isNotEmpty()) RustWebLogin.cancel(pendingId)
            RustSessionStore.delete(id)
            RustSystemState.setTwoFactor(oldTwoFactor)
            UserPrefs.desktopAccess.set(oldDesktop)
            UserPrefs.service.set(oldService)
        }
    }
}
