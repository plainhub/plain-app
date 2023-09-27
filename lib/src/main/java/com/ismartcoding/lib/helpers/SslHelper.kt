package com.ismartcoding.lib.helpers

import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SslHelper {
    fun disableSSLCertificateChecking() {
        val hostnameVerifier = HostnameVerifier { _, _ -> true }
        val trustAllCerts =
            arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }

                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        arg0: Array<X509Certificate>,
                        arg1: String,
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        arg0: Array<X509Certificate>,
                        arg1: String,
                    ) {
                    }
                },
            )
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier)
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }
}
