package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.helpers.StringHelper
import com.ismartcoding.plain.lib.crypto.hmacSha256
import com.ismartcoding.plain.platform.generateChaCha20Key
import com.ismartcoding.plain.preferences.MasterSecretPreference
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Token crypto for shared file links.
 *
 * - `shared_id` (public, in the link path) and `shared_token` (derived
 *   `HMAC-SHA256(masterSecret, shared_id)`, never stored) mirror the
 *   pairing/client_id model.
 * - `url_token` is a per-share random key used to encrypt guest `/fs` / `/zip/dir` ids.
 */
object ShareCrypto {
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun masterSecret(): ByteArray {
        return Base64.decode(MasterSecretPreference.ensureValueAsync())
    }

    /**
     * Derive the 32-byte `shared_token` from [sharedId] using the given
     * [secret]; equal on the sharing device (which has [masterSecret]) and on
     * the server given the same [sharedId]. The client never stores it — it is
     * recomputed per request.
     */
    fun deriveSharedToken(secret: ByteArray, sharedId: String): ByteArray {
        return hmacSha256(secret, sharedId.encodeToByteArray())
    }

    /** Derive the `shared_token` from the stored [masterSecret]. */
    suspend fun deriveSharedToken(sharedId: String): ByteArray {
        return deriveSharedToken(masterSecret(), sharedId)
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun deriveSharedTokenEncoded(sharedId: String): String {
        return Base64.UrlSafe.encode(deriveSharedToken(sharedId))
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun deriveSharedTokenEncoded(secret: ByteArray, sharedId: String): String {
        return Base64.UrlSafe.encode(deriveSharedToken(secret, sharedId))
    }

    /** Generate a fresh random `shared_id`, reusing the `client_id` scheme. */
    fun newSharedId(): String = StringHelper.shortUUID()

    /** Generate a fresh random `url_token`. */
    fun newUrlToken(): String = generateChaCha20Key()
}