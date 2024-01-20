package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.bean.CertificateMeta
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.Selector
import java.security.Provider
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

internal class BCCertificateParser(data: ByteArray) : CertificateParser(data) {
    override fun parse(): List<CertificateMeta> {
        val cmsSignedData: CMSSignedData = try {
            CMSSignedData(data)
        } catch (e: CMSException) {
            throw CertificateException(e)
        }
        val certStore = cmsSignedData.certificates
        val signerInfos = cmsSignedData.signerInfos
        val signers = signerInfos.signers
        val certificates: MutableList<X509Certificate?> = ArrayList()
        for (signer in signers) {
            val signerId : Selector<X509CertificateHolder> = signer.sid as Selector<X509CertificateHolder>
            val matches = certStore.getMatches(signerId)
            for (holder in matches) {
                certificates.add(
                    JcaX509CertificateConverter().setProvider(provider).getCertificate(holder)
                )
            }
        }
        return CertificateMetas.from(certificates)
    }

    companion object {
        private val provider: Provider = BouncyCastleProvider()
    }
}