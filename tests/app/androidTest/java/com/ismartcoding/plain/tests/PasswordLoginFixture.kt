package com.ismartcoding.plain.tests

import com.google.crypto.tink.subtle.Hkdf
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.crypto.sha256
import com.ismartcoding.plain.network.TestWebSocketSession
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Decrypt
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Encrypt
import com.ismartcoding.plain.testing.CryptoFixtures.computeECDHSharedKey
import com.ismartcoding.plain.testing.CryptoFixtures.generateECDHKeyPair
import com.ismartcoding.plain.testing.CryptoFixtures.verifyEd25519Signature
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

private const val PASSWORD_LOGIN_DOMAIN = "plain-web-password-login-v1"

internal suspend fun TestWebSocketSession.beginPasswordLogin(
    clientId: String,
    signaturePublicKey: String,
    password: String,
): PasswordLoginFixture {
    val ephemeral = generateECDHKeyPair()
    val reveal = LoginReveal(
        Base64Lenient.encode(ephemeral.publicKeyEncoded),
        Base64Lenient.encode(ByteArray(32).also { SecureRandom().nextBytes(it) }),
    )
    val commitmentFields = listOf(PASSWORD_LOGIN_DOMAIN, clientId, reveal.publicKey, reveal.nonce)
    val commitment = Base64Lenient.encode(sha256(JsonHelper.jsonEncode(commitmentFields).encodeToByteArray()))
    sendText(JsonHelper.jsonEncode(LoginHello(commitment)))
    val hello = JsonHelper.jsonDecode<SignedLoginHello>(
        requireNotNull(withTimeout(10_000) { incoming.receive() }.text) { "Expected a signed login hello" },
    )
    check(hello.clientId == clientId && hello.clientCommitment == commitment && hello.deviceId.isNotEmpty())
    check(hello.expiresAt > System.currentTimeMillis()) { "Login hello expired" }
    val fields = listOf(
        PASSWORD_LOGIN_DOMAIN, hello.clientId, hello.deviceId, hello.clientCommitment, hello.serverPublicKey, hello.serverNonce,
    ).map { JsonPrimitive(it) } + JsonPrimitive(hello.expiresAt)
    val signingKey = Base64Lenient.decode(signaturePublicKey)
    check(verifyEd25519Signature(signingKey, JsonHelper.jsonEncode(fields).encodeToByteArray(), Base64Lenient.decode(hello.signature))) {
        "Invalid login hello signature"
    }
    val transcript = fields + listOf(JsonPrimitive(reveal.publicKey), JsonPrimitive(reveal.nonce))
    val context = sha256(JsonHelper.jsonEncode(transcript).encodeToByteArray())
    val shared = requireNotNull(computeECDHSharedKey(ephemeral.privateKeyEncoded, Base64Lenient.decode(hello.serverPublicKey)))
        .let(Base64Lenient::decode)
    val requestKey = Hkdf.computeHkdf("HmacSHA256", shared, context, "$PASSWORD_LOGIN_DOMAIN/request".encodeToByteArray(), 32)
    val responseKey = Hkdf.computeHkdf("HmacSHA256", shared, context, "$PASSWORD_LOGIN_DOMAIN/response".encodeToByteArray(), 32)

    val session = generateECDHKeyPair()
    val digest = MessageDigest.getInstance("SHA-512").digest(password.encodeToByteArray())
    val request = PasswordLoginRequest(
        password = digest.joinToString("") { "%02x".format(it) },
        loginContext = Base64Lenient.encode(context),
        ecdhPublicKey = Base64Lenient.encode(session.publicKeyEncoded),
    )
    sendText(JsonHelper.jsonEncode(reveal))
    sendBinary(chaCha20Encrypt(requestKey, JsonHelper.jsonEncode(request)))
    return PasswordLoginFixture(this, hello.deviceId, request.loginContext, signingKey, responseKey, session.privateKeyEncoded)
}

internal class PasswordLoginFixture(
    private val socket: TestWebSocketSession,
    private val deviceId: String,
    private val context: String,
    private val signingKey: ByteArray,
    private val responseKey: ByteArray,
    private val sessionPrivateKey: ByteArray,
) {
    suspend fun expectPending() {
        check(receiveReply().status == "PENDING") { "Expected pending login approval" }
    }

    suspend fun sessionToken(): ByteArray {
        val reply = receiveReply()
        check(reply.status == "COMPLETED" && reply.loginContext == context && !reply.chatPaired) {
            "Expected login completion for the negotiated channel"
        }
        val signed = "$deviceId|${reply.status}|${reply.ecdhPublicKey}|${reply.timestamp}|$context"
        check(verifyEd25519Signature(signingKey, signed.encodeToByteArray(), Base64Lenient.decode(reply.signature))) {
            "Invalid login completion signature"
        }
        val token = requireNotNull(computeECDHSharedKey(sessionPrivateKey, Base64Lenient.decode(reply.ecdhPublicKey)))
        return Base64Lenient.decode(token)
    }

    private suspend fun receiveReply(): LoginReply {
        val frame = requireNotNull(withTimeout(10_000) { socket.incoming.receive() }.binary) { "Expected an encrypted login reply" }
        val bytes = requireNotNull(chaCha20Decrypt(responseKey, frame)) { "Could not authenticate the login reply" }
        return JsonHelper.jsonDecode<LoginReply>(bytes.decodeToString()).also {
            check(it.clientId == deviceId) { "Login reply belongs to a different device" }
        }
    }
}

@Serializable
private data class LoginHello(val commitment: String)

@Serializable
private data class LoginReveal(val publicKey: String, val nonce: String)

@Serializable
private data class SignedLoginHello(
    val clientId: String,
    val deviceId: String,
    val clientCommitment: String,
    val serverPublicKey: String,
    val serverNonce: String,
    val expiresAt: Long,
    val signature: String,
)

@Serializable
private data class PasswordLoginRequest(
    val password: String,
    val loginContext: String,
    val ecdhPublicKey: String,
    val appName: String = "Fixture",
    val appVersion: String = "1",
    val osName: String = "Android",
    val osVersion: String = "1",
    val isMobile: Boolean = true,
)

@Serializable
private data class LoginReply(
    val clientId: String,
    val status: String,
    val loginContext: String = "",
    val ecdhPublicKey: String = "",
    val timestamp: Long = 0,
    val signature: String = "",
    val chatPaired: Boolean = false,
)
