package com.ismartcoding.lib.apk.bean

class ApkSigner(
    val path: String,
    val certificateMetas: List<CertificateMeta?>
) {

    override fun toString(): String {
        return "ApkSigner{" +
                "path='" + path + '\'' +
                ", certificateMetas=" + certificateMetas +
                '}'
    }
}