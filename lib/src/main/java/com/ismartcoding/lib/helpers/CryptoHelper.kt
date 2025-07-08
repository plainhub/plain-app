package com.ismartcoding.lib.helpers

import android.util.Base64
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.signature.SignatureConfig
import com.google.crypto.tink.signature.SignatureKeyTemplates
import com.ismartcoding.lib.logcat.LogCat
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.Random
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    
    @Volatile
    private var tinkInitialized = false
    
    /**
     * Initialize Google Tink for Ed25519 signatures
     */
    private fun initializeTink() {
        if (!tinkInitialized) {
            synchronized(this) {
                if (!tinkInitialized) {
                    try {
                        SignatureConfig.register()
                        tinkInitialized = true
                        LogCat.d("Google Tink initialized successfully (Ed25519 signatures)")
                    } catch (ex: Exception) {
                        LogCat.e("Failed to initialize Google Tink: ${ex.message}")
                        throw ex
                    }
                }
            }
        }
    }
    
    fun sha512(input: ByteArray) = hashString("SHA-512", input)

    fun sha256(input: ByteArray) = hashString("SHA-256", input)

    fun sha1(input: ByteArray) = hashString("SHA-1", input)

    private fun hashString(
        type: String,
        input: ByteArray,
    ): String {
        val bytes =
            MessageDigest
                .getInstance(type)
                .digest(input)

        return bytesToHash(bytes)
    }

    private fun bytesToHash(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    fun sha256(path: Path): String {
        val dig =
            MessageDigest
                .getInstance("SHA-256")
        RandomAccessFile(path.toFile(), "r").use { rafile ->
            val fileChannel = rafile.channel
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
            var start: Long = 0
            var len = Files.size(path)
            val MAX_SIZE = 4096 * 128
            while (start < len) {
                val remaining = len - start
                val bufferSize = if (remaining < MAX_SIZE) remaining.toInt() else MAX_SIZE
                val dst = ByteArray(bufferSize)
                buffer.get(dst)
                dig.update(dst)
                start += bufferSize.toLong()
            }
            return bytesToHash(dig.digest())
        }
    }

    fun chaCha20Encrypt(
        key: String,
        content: ByteArray,
    ): ByteArray {
        return chaCha20Encrypt(Base64.decode(key, Base64.NO_WRAP), content)
    }

    fun chaCha20Encrypt(
        key: ByteArray,
        content: String,
    ): ByteArray {
        return chaCha20Encrypt(key, content.toByteArray())
    }

    fun chaCha20Encrypt(
        key: ByteArray,
        content: ByteArray,
    ): ByteArray {
        return try {
            require(key.size == 32) { "ChaCha20 key must be 32 bytes, got ${key.size}" }
            
            // Generate random 12-byte nonce for ChaCha20-Poly1305
            val nonce = ByteArray(12)
            SecureRandom.getInstanceStrong().nextBytes(nonce)
            
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val secretKey = SecretKeySpec(key, "ChaCha20")
            val ivSpec = IvParameterSpec(nonce) // Use IvParameterSpec for ChaCha20-Poly1305
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val ciphertext = cipher.doFinal(content)
            
            nonce + ciphertext
        } catch (ex: Exception) {
            LogCat.e("Failed to encrypt with ChaCha20-Poly1305: ${ex.message}")
            throw ex
        }
    }

    fun chaCha20Encrypt(
        key: String,
        content: String,
    ): ByteArray {
        return chaCha20Encrypt(key, content.toByteArray())
    }
    


    fun chaCha20Decrypt(
        key: String,
        content: ByteArray,
    ): ByteArray? {
        return chaCha20Decrypt(Base64.decode(key, Base64.NO_WRAP), content)
    }

    fun chaCha20Decrypt(
        key: ByteArray,
        content: ByteArray,
    ): ByteArray? {
        return try {
            require(key.size == 32) { "ChaCha20 key must be 32 bytes, got ${key.size}" }
            require(content.size >= 12) { "Content must be at least 12 bytes (nonce + ciphertext)" }
            
            // Extract nonce and ciphertext
            val nonce = content.copyOfRange(0, 12)
            val ciphertext = content.copyOfRange(12, content.size)
            
            val cipher = Cipher.getInstance("ChaCha20-Poly1305")
            val secretKey = SecretKeySpec(key, "ChaCha20")
            val ivSpec = IvParameterSpec(nonce) // Use IvParameterSpec for ChaCha20-Poly1305
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            cipher.doFinal(ciphertext)
        } catch (ex: Exception) {
            LogCat.e("Failed to decrypt with ChaCha20-Poly1305: ${ex.message}")
            null
        }
    }
    


    fun generateChaCha20Key(): String {
        val bytes = ByteArray(32) // ChaCha20 uses 32-byte keys
        SecureRandom.getInstanceStrong().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    


    fun randomPassword(n: Int): String {
        val characterSet = "23456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ"

        val random = Random(System.nanoTime())
        val password = StringBuilder()

        for (i in 0 until n) {
            val rIndex = random.nextInt(characterSet.length)
            password.append(characterSet[rIndex])
        }

        return password.toString()
    }

    /**
     * Generate ECDH key pair for secure pairing
     */
    fun generateECDHKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Compute shared ChaCha20 key using ECDH protocol
     * @param privateKey Our private key
     * @param publicKeyBytes Other party's public key bytes
     * @return Base64 encoded ChaCha20 key or null if failed
     */
    fun computeECDHSharedKey(privateKey: PrivateKey, publicKeyBytes: ByteArray): String? {
        return try {
            // Reconstruct the public key from bytes
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            
            // Perform ECDH key agreement
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(publicKey, true)
            
            // Generate shared secret
            val sharedSecret = keyAgreement.generateSecret()
            
            // Use SHA-256 to derive ChaCha20 key from shared secret
            val digest = MessageDigest.getInstance("SHA-256")
            val chaCha20KeyBytes = digest.digest(sharedSecret)
            
            // Return Base64 encoded ChaCha20 key
            Base64.encodeToString(chaCha20KeyBytes, Base64.NO_WRAP)
        } catch (ex: Exception) {
            LogCat.e("ECDH key computation failed: ${ex.message}")
            null
        }
    }

    /**
     * Get public key bytes for transmission
     */
    fun getPublicKeyBytes(keyPair: KeyPair): ByteArray {
        return keyPair.public.encoded
    }

    /**
     * Ed25519 key pair with Tink handles for signing/verifying
     */
    data class Ed25519KeyPair(
        val privateKeyBytes: ByteArray,  // Tink private KeysetHandle serialized as JSON bytes
        val publicKey: String            // Tink public KeysetHandle serialized as JSON bytes, Base64 encoded
    )
    
    /**
     * Generate Ed25519 key pair using Google Tink (supports all Android versions)
     */
    fun generateEd25519KeyPair(): Ed25519KeyPair {
        initializeTink()
        
        try {
            // Generate private key using Ed25519 key template
            val privateKeyHandle = KeysetHandle.generateNew(SignatureKeyTemplates.ED25519)
            
            // Get public key
            val publicKeyHandle = privateKeyHandle.publicKeysetHandle
            
            // Serialize keys to JSON for storage
            val privateKeyOutputStream = ByteArrayOutputStream()
            CleartextKeysetHandle.write(privateKeyHandle, JsonKeysetWriter.withOutputStream(privateKeyOutputStream))
            val privateKeyBytes = privateKeyOutputStream.toByteArray()
            
            val publicKeyOutputStream = ByteArrayOutputStream()
            CleartextKeysetHandle.write(publicKeyHandle, JsonKeysetWriter.withOutputStream(publicKeyOutputStream))
            val publicKeyBytesRaw = publicKeyOutputStream.toByteArray()
            
            return Ed25519KeyPair(
                privateKeyBytes = privateKeyBytes,
                publicKey = Base64.encodeToString(publicKeyBytesRaw, Base64.NO_WRAP)
            )
        } catch (ex: Exception) {
            LogCat.e("Failed to generate Ed25519 key pair with Tink: ${ex.message}")
            throw ex
        }
    }

    /**
     * Sign data using raw Ed25519 private key (32 bytes)
     */
    fun signDataWithRawEd25519PrivateKey(rawPrivateKey: ByteArray, data: ByteArray): ByteArray? {
        return try {
            require(rawPrivateKey.size == 32) { "Ed25519 private key must be 32 bytes, got ${rawPrivateKey.size}" }
            initializeTink()

            // Use Tink's Ed25519Signer directly with the raw private key
            val signer = com.google.crypto.tink.subtle.Ed25519Sign(rawPrivateKey)
            signer.sign(data)
        } catch (ex: Exception) {
            LogCat.e("signDataWithRawEd25519PrivateKey: ${ex.message}")
            null
        }
    }

    /**
     * Extract raw Ed25519 public key (32 bytes) from Tink KeysetHandle
     * @param publicKeyBytes Tink public KeysetHandle serialized as JSON bytes, Base64 encoded
     * @return Raw Ed25519 public key (32 bytes) or null if failed
     */
    fun extractRawEd25519PublicKey(publicKeyBytes: String): ByteArray? {
        return try {
            val publicKeyBytesDecoded = Base64.decode(publicKeyBytes, Base64.NO_WRAP)
            val jsonString = String(publicKeyBytesDecoded, Charsets.UTF_8)
            val jsonObject = org.json.JSONObject(jsonString)
            
            val keyArray = jsonObject.getJSONArray("key")
            val firstKey = keyArray.getJSONObject(0)
            val keyData = firstKey.getJSONObject("keyData")
            val keyValueBase64 = keyData.getString("value")
            
            val keyValueBytes = Base64.decode(keyValueBase64, Base64.NO_WRAP)
            
            // Ed25519PublicKey protobuf format: 0x12 0x20 + 32_bytes_key (field 2)
            if (keyValueBytes.size >= 34 && keyValueBytes[0].toInt() == 0x12 && keyValueBytes[1].toInt() == 0x20) {
                return keyValueBytes.copyOfRange(2, 34) // Extract 32 bytes
            }
            
            null
        } catch (ex: Exception) {
            LogCat.e("Failed to extract raw Ed25519 public key: ${ex.message}")
            null
        }
    }

    fun verifySignatureWithRawEd25519PublicKey(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            initializeTink()
            val verifier = com.google.crypto.tink.subtle.Ed25519Verify(publicKey)
            verifier.verify(signature, data)
            true
        } catch (ex: Exception) {
            false
        }
    }

    fun encodeKeyToBase64(key: Key): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun encodeToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
