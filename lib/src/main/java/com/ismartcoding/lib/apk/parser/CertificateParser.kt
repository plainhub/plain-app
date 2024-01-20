package com.ismartcoding.lib.apk.parser

import com.ismartcoding.lib.apk.ApkParsers
import com.ismartcoding.lib.apk.bean.CertificateMeta

abstract class CertificateParser(protected val data: ByteArray) {
    abstract fun parse(): List<CertificateMeta>

    companion object {
        fun getInstance(data: ByteArray): CertificateParser {
            return if (ApkParsers.useBouncyCastle()) {
                BCCertificateParser(data)
            } else JSSECertificateParser(data)
        }
    }
}