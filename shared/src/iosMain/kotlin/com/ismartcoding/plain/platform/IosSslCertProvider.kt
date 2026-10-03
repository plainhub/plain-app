package com.ismartcoding.plain.platform

interface IosSslCertProvider {
    fun certificatePem(): String
    fun privateKeyPem(): String
    fun getCertSignatureBytes(): ByteArray
    fun regenerateCert(): ByteArray
    @Throws(Exception::class)
    fun replaceCertWithPkcs12(p12Data: ByteArray, password: String): ByteArray
    @Throws(Exception::class)
    fun replaceCertWithPem(certPem: String, keyPem: String): ByteArray
}
