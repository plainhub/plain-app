package com.ismartcoding.plain.lib.apk.cert.x509

import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1DerEncoder
import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1OpaqueObject
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.util.Date

/**
 * Holder for a freshly generated self-signed certificate and its private key.
 */
class SelfSignedCertificate(
    val certificate: X509Certificate,
    val privateKey: java.security.PrivateKey,
)

/**
 * Generates an EC P-256 self-signed X.509 v3 certificate by hand, using the
 * plain DER encoder for the certificate structure and the platform crypto
 * providers (KeyPairGenerator / Signature / CertificateFactory) for the EC key,
 * the SHA256withECDSA signature and final certificate parsing. No BouncyCastle.
 */
object X509SelfSignedGenerator {
    private const val SIG_ALGORITHM = "SHA256withECDSA"
    // Android's EC provider recognizes "P-256", but the desktop JVM (host unit
    // tests) does not. The OID is understood by both, so use it as the source
    // of truth for the curve parameter.
    private const val CURVE_PARAMETER = X509Oid.CURVE_P256
    private const val KEY_SIZE_BYTES = 32
    private const val DEFAULT_VALIDITY_YEARS = 20L

    private val GENERALIZED_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'").withZone(ZoneOffset.UTC)

    /**
     * Generate a self-signed certificate for [commonName], valid from [notBefore]
     * to [notAfter], wrapped together with its EC private key.
     */
    fun newSelfSigned(commonName: String, notBefore: Date, notAfter: Date): SelfSignedCertificate {
        val keyPair = newEcKeyPair()
        val tbsDer = buildTbsCertificate(keyPair, commonName, notBefore, notAfter)
        val signature = sign(tbsDer, keyPair.private)
        val certDer = Asn1DerEncoder.encode(
            X509CertificateStructure(
                Asn1OpaqueObject(tbsDer),
                EcdsaSignatureAlgorithm(X509Oid.SHA256_WITH_ECDSA, Asn1OpaqueObject(X509Oid.NULL_DER)),
                signature,
            ),
        )!!
        val x509 = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certDer)) as X509Certificate
        return SelfSignedCertificate(x509, keyPair.private)
    }

    /**
     * Generate a self-signed certificate and store it under [alias] in a fresh
     * platform PKCS#12 [KeyStore] protected by [password].
     */
    fun newSelfSignedKeyStore(alias: String, password: String, commonName: String): KeyStore {
        val now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
        val generated = newSelfSigned(
            commonName,
            Date.from(now),
            Date.from(now.plusSeconds(DEFAULT_VALIDITY_YEARS * 365L * 24L * 60L * 60L)),
        )
        return keyStoreOf(alias, password, generated)
    }

    /**
     * Ship [generated] as a platform PKCS#12 [KeyStore] under [alias], protected by [password].
     */
    fun keyStoreOf(alias: String, password: String, generated: SelfSignedCertificate): KeyStore =
        KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(alias, generated.privateKey, password.toCharArray(), arrayOf<Certificate>(generated.certificate))
        }

    private fun newEcKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(CURVE_PARAMETER))
        return generator.generateKeyPair()
    }

    private fun buildTbsCertificate(keyPair: KeyPair, commonName: String, notBefore: Date, notAfter: Date): ByteArray {
        val tbs = TbsCertificate().apply {
            version = 2 // v3
            serialNumber = BigInteger.valueOf(System.nanoTime())
            signature = EcdsaSignatureAlgorithm(X509Oid.SHA256_WITH_ECDSA, Asn1OpaqueObject(X509Oid.NULL_DER))
            issuer = X500Name(commonName)
            validity = Validity(generalizedTime(notBefore), generalizedTime(notAfter))
            subject = X500Name(commonName)
            subjectPublicKeyInfo = subjectPublicKeyInfo(keyPair.public as ECPublicKey)
        }
        return Asn1DerEncoder.encode(tbs)!!
    }

    private fun generalizedTime(date: Date): String = GENERALIZED_TIME.format(date.toInstant())

    private fun subjectPublicKeyInfo(publicKey: ECPublicKey): SubjectPublicKeyInfo {
        val point = publicKey.w
        val encoded = byteArrayOf(0x04) +
            toFixedBytes(point.affineX) + toFixedBytes(point.affineY)
        return SubjectPublicKeyInfo(
            EcPublicKeyAlgorithm(X509Oid.EC_PUBLIC_KEY, X509Oid.CURVE_P256),
            encoded,
        )
    }

    private fun toFixedBytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        if (raw.size == KEY_SIZE_BYTES) return raw
        val padded = ByteArray(KEY_SIZE_BYTES)
        // Copy the least-significant KEY_SIZE_BYTES bytes into the tail of the padded array.
        if (raw.size < KEY_SIZE_BYTES) {
            System.arraycopy(raw, 0, padded, KEY_SIZE_BYTES - raw.size, raw.size)
        } else {
            // Strip the leading sign byte (or excess) from a full-precision value.
            System.arraycopy(raw, raw.size - KEY_SIZE_BYTES, padded, 0, KEY_SIZE_BYTES)
        }
        return padded
    }

    private fun sign(tbsDer: ByteArray, privateKey: java.security.PrivateKey): ByteArray {
        val signature = Signature.getInstance(SIG_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(tbsDer)
        return signature.sign()
    }
}