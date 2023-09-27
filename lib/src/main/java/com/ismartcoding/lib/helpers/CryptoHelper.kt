package com.ismartcoding.lib.helpers

import android.security.keystore.KeyProperties
import android.util.Base64
import com.ismartcoding.lib.logcat.LogCat
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
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

    fun aesEncrypt(
        key: String,
        content: ByteArray,
    ): ByteArray {
        return aesEncrypt(Base64.decode(key, Base64.NO_WRAP), content)
    }

    fun aesEncrypt(
        key: ByteArray,
        content: String,
    ): ByteArray {
        return aesEncrypt(key, content.toByteArray())
    }

    fun aesEncrypt(
        key: ByteArray,
        content: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        val iv = ByteArray(12)
        SecureRandom.getInstanceStrong().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        return iv + cipher.doFinal(content)
    }

    fun aesEncrypt(
        key: String,
        content: String,
    ): ByteArray {
        return aesEncrypt(key, content.toByteArray())
    }

    fun aesDecrypt(
        key: String,
        content: ByteArray,
    ): ByteArray? {
        return aesDecrypt(Base64.decode(key, Base64.NO_WRAP), content)
    }

    fun aesDecrypt(
        key: ByteArray,
        content: ByteArray,
    ): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = SecretKeySpec(key, "AES")
            val iv = content.copyOfRange(0, 12)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            cipher.doFinal(content.copyOfRange(12, content.size))
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            null
        }
    }

    fun rsaEncrypt(
        content: String,
        publicKey: String,
    ): ByteArray? {
        val publicBytes = Base64.decode(publicKey, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKey = keyFactory.generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        return cipher.doFinal(content.toByteArray())
    }

    fun rsaDecrypt(
        content: ByteArray,
        privateKey: PrivateKey,
    ): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(content).decodeToString()
    }

    fun generateRsaKey(): KeyPair {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    fun generateAESKey(): String {
        val bytes = ByteArray(32)
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
}
