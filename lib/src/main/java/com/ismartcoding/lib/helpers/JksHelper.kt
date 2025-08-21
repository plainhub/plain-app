package com.ismartcoding.lib.helpers

import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.ECGenParameterSpec
import java.util.*

object JksHelper {
    fun genJksFile(
        alias: String,
        password: String,
        commonName: String,
    ): KeyStore {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC")
        keyPairGenerator.initialize(ECGenParameterSpec("P-256"))

        val keyPair = keyPairGenerator.genKeyPair()

        val keyStore = KeyStore.getInstance("BKS", "BC")
        keyStore.load(null, null)

        val x509CertificateHolder = createTrustHolder(keyPair, "SHA256withECDSA", commonName)
        val x509Certificate = JcaX509CertificateConverter().setProvider("BC").getCertificate(x509CertificateHolder)
        keyStore.setKeyEntry(alias, keyPair.private, password.toCharArray(), arrayOf<Certificate>(x509Certificate))

        return keyStore
    }

    private fun createTrustHolder(
        keyPair: KeyPair,
        sigAlg: String,
        commonName: String,
    ): X509CertificateHolder {
        val x500NameBuilder = X500NameBuilder(BCStyle.INSTANCE)
        val x500Name =
            x500NameBuilder
                .addRDN(BCStyle.CN, commonName)
                .addRDN(BCStyle.O, commonName)
                .build()
        val certificateBuilder =
            X509v3CertificateBuilder(
                x500Name,
                BigInteger.valueOf(System.currentTimeMillis()),
                calculateDate(0),
                calculateDate(24 * 365 * 20),
                Locale.ENGLISH,
                x500Name,
                SubjectPublicKeyInfo.getInstance(
                    keyPair.public.encoded,
                ),
            )
        val contentSigner = JcaContentSignerBuilder(sigAlg).setProvider("BC").build(keyPair.private)
        return certificateBuilder.build(contentSigner)
    }

    private fun calculateDate(hoursInFuture: Int): Date {
        val secondsNow = System.currentTimeMillis() / 1000
        return Date((secondsNow + hoursInFuture.toLong() * 60 * 60) * 1000)
    }

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}
