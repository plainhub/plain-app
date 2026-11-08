package com.ismartcoding.plain.tests

import androidx.compose.runtime.snapshotFlow
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.sessions.RustSessionStore
import com.ismartcoding.plain.features.sessions.RustWebLogin
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.sessions.*
import com.ismartcoding.plain.network.*
import com.ismartcoding.plain.rust.RustRuntime
import com.ismartcoding.plain.ui.app.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionLoginRustHttpTest {
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
            val port = Prefs.httpPort.value
            val fixturePassword = requireNotNull(InstrumentationRegistry.getArguments().getString("loginPassword")) { "Supply the owned fixture loginPassword argument" }
            val bootstrap = http.post("http://127.0.0.1:$port/init", ByteArray(0), headers = mapOf("c-id" to id)).use {
                assertEquals(200, it.status.value)
                JsonHelper.jsonDecode<LoginBootstrap>(it.bodyAsText())
            }
            assertTrue(bootstrap.passwordRequired)
            assertFalse(bootstrap.authenticated)
            val notification = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    snapshotFlow { MainViewModel.pendingLoginRequest.value }.first { it?.clientId == id }!!
                }
            }
            http.webSocket("ws://127.0.0.1:$port/auth?cid=$id&auth=1") { socket ->
                val login = socket.beginPasswordLogin(id, bootstrap.signaturePublicKey, fixturePassword)
                login.expectPending()
                val facts = notification.await()
                pendingId = facts.requestId
                val pendingRequest = facts.request
                assertEquals("", pendingRequest.password)
                assertEquals("Fixture", pendingRequest.appName)
                assertTrue(RustSessionStore.list().none { it.clientId == id })
                RustWebLogin.complete(pendingId)
                val token = login.sessionToken()
                val session = RustSessionStore.list().single { it.clientId == id }
                assertEquals("Fixture", session.appName)
                assertFalse(session.isApiToken)
                assertArrayEquals(token, Base64Lenient.decode(session.token))
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

@Serializable
private data class LoginBootstrap(
    val signaturePublicKey: String,
    val passwordRequired: Boolean,
    val authenticated: Boolean,
)
