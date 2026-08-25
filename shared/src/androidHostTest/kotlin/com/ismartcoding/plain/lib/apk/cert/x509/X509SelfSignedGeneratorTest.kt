package com.ismartcoding.plain.lib.apk.cert.x509

import java.security.Signature
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the BC-free [X509SelfSignedGenerator].
 *
 * The generated certificate is validated entirely through the platform crypto
 * stack, the same way the HTTPS layer will consume it:
 *  - Platform [X509Certificate] parsing of our hand-built DER.
 *  - `SHA256withECDSA` signature verification of the TBS bytes.
 *  - Subject/issuer, validity window and serial sanity checks.
 */
class X509SelfSignedGeneratorTest {

    private val notBefore = Date.from(Instant.parse("2026-01-01T00:00:00Z"))
    private val notAfter = Date.from(Instant.parse("2040-01-01T00:00:00Z"))

    private fun generate(notBefore: Date = this.notBefore, notAfter: Date = this.notAfter) =
        X509SelfSignedGenerator.newSelfSigned("Plain", notBefore, notAfter)

    @Test
    fun parsesAsPlatformX509() {
        val generated = generate()
        assertNotNull(generated.privateKey)
        assertTrue(generated.certificate is X509Certificate)
        assertEquals("X.509", generated.certificate.type)
    }

    @Test
    fun subjectAndIssuerUseCommonName() {
        val cert = generate().certificate
        // Both subject and issuer are CN=Plain,O=Plain (RFC 2253: ',' separates multi-value RDN,
        // but Sun's canonical form uses '+').
        assertTrue(cert.subjectX500Principal.name.contains("CN=Plain"))
        assertTrue(cert.subjectX500Principal.name.contains("O=Plain"))
        assertTrue(cert.issuerX500Principal.name.contains("CN=Plain"))
        assertTrue(cert.issuerX500Principal.name.contains("O=Plain"))
    }

    @Test
    fun versionIsV3() {
        val version = generate().certificate.version
        assertEquals(3, version)
    }

    @Test
    fun serNumberPositive() {
        assertTrue(generate().certificate.serialNumber.signum() > 0)
    }

    @Test
    fun validityWindowRoundsTrip() {
        val cert = generate().certificate
        assertEquals(notBefore.time, cert.notBefore.time)
        assertEquals(notAfter.time, cert.notAfter.time)
    }

    @Test
    fun isSelfSigned() {
        assertTrue(generate().certificate.subjectX500Principal == generate().certificate.issuerX500Principal)
    }

    @Test
    fun defaultSigAlgIsSha256WithEcdsa() {
        val cert = generate().certificate
        assertTrue(
            cert.sigAlgName.contains("SHA256withECDSA", ignoreCase = true),
            "expected sha256withECDSA but was ${cert.sigAlgName}",
        )
    }

    @Test
    fun publicKeyIsEcP256() {
        val pubKey = generate().certificate.publicKey
        assertEquals("EC", pubKey.algorithm)
        val ecKey = pubKey as java.security.interfaces.ECPublicKey
        val fieldSize = ecKey.params.curve.field.fieldSize
        assertEquals(256, fieldSize)
    }

    @Test
    fun signatureVerifiesAgainstPublicKey() {
        val cert = generate().certificate
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(cert.publicKey)
        sig.update(cert.tbsCertificate)
        assertTrue(sig.verify(cert.signature), "self-signature must verify against the embedded public key")
    }

    @Test
    fun freshCertificatesDiffer() {
        val c1 = generate().certificate
        val c2 = generate().certificate
        assertTrue(c1.serialNumber != c2.serialNumber)
        assertTrue(!c1.signature.contentEquals(c2.signature))
    }

    @Test
    fun keyStoreRoundTrip() {
        val keystore = X509SelfSignedGenerator.newSelfSignedKeyStore("alias", "password", "Plain")
        val privateKey = keystore.getKey("alias", "password".toCharArray())!!
        val chain = keystore.getCertificateChain("alias")!!
        assertEquals("EC", privateKey.algorithm)
        assertEquals(1, chain.size)
    }
}