package com.ismartcoding.lib.apk.bean

import java.util.Objects

/**
 * ApkSignV1 certificate file.
 */
class ApkSigner(
    /**
     * The cert file path in apk file
     */
    val path: String, certificateMetas: List<CertificateMeta?>?
) {

    /**
     * The meta info of certificate contained in this cert file.
     */
    val certificateMetas: List<CertificateMeta?>

    init {
        this.certificateMetas = Objects.requireNonNull(certificateMetas)!!
    }

    override fun toString(): String {
        return "ApkSigner{" +
                "path='" + path + '\'' +
                ", certificateMetas=" + certificateMetas +
                '}'
    }
}