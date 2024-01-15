package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.bean.CertificateMeta
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate
import java.util.Locale

object CertificateMetas {
    @Throws(CertificateEncodingException::class)
    fun from(certificates: List<X509Certificate?>?): List<CertificateMeta> {
        val certificateMetas: MutableList<CertificateMeta> = ArrayList(
            certificates!!.size
        )
        for (certificate in certificates) {
            val certificateMeta = from(certificate)
            certificateMetas.add(certificateMeta)
        }
        return certificateMetas
    }

    @Throws(CertificateEncodingException::class)
    fun from(certificate: X509Certificate?): CertificateMeta {
        val bytes = certificate!!.encoded
        val certMd5 = md5Digest(bytes)
        val publicKeyString = byteToHexString(bytes)
        val certBase64Md5 = md5Digest(publicKeyString)
        return CertificateMeta(
            certificate.sigAlgName.uppercase(Locale.getDefault()),
            certificate.sigAlgOID,
            certificate.notBefore,
            certificate.notAfter,
            bytes, certBase64Md5, certMd5
        )
    }

    private fun md5Digest(input: ByteArray): String {
        val digest = getDigest("md5")
        digest.update(input)
        return getHexString(digest.digest())
    }

    private fun md5Digest(input: String): String {
        val digest = getDigest("md5")
        digest.update(input.toByteArray(StandardCharsets.UTF_8))
        return getHexString(digest.digest())
    }

    private fun byteToHexString(bArray: ByteArray): String {
        val sb = StringBuilder(bArray.size)
        var sTemp: String
        for (aBArray in bArray) {
            sTemp = Integer.toHexString(0xFF and Char(aBArray.toUShort()).code)
            if (sTemp.length < 2) {
                sb.append(0)
            }
            sb.append(sTemp.uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }

    private fun getHexString(digest: ByteArray): String {
        val bi = BigInteger(1, digest)
        return String.format("%032x", bi)
    }

    private fun getDigest(algorithm: String): MessageDigest {
        return try {
            MessageDigest.getInstance(algorithm)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e.message)
        }
    }
}