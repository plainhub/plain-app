package com.ismartcoding.lib.apk.bean

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date

class CertificateMeta(
    /**
     * the sign algorithm name
     */
    val signAlgorithm: String,
    /**
     * the signature algorithm OID string.
     * An OID is represented by a set of non-negative whole numbers separated by periods.
     * For example, the string "1.2.840.10040.4.3" identifies the SHA-1 with DSA signature algorithm defined in
     * [
 * RFC 3279: Algorithms and Identifiers for the Internet X.509 Public Key Infrastructure Certificate and CRL Profile
](http://www.ietf.org/rfc/rfc3279.txt) * .
     */
    val signAlgorithmOID: String,
    /**
     * the start date of the validity period.
     */
    val startDate: Date,
    /**
     * the end date of the validity period.
     */
    val endDate: Date,
    /**
     * certificate binary data.
     */
    val data: ByteArray,
    /**
     * first use base64 to encode certificate binary data, and then calculate md5 of base64b string.
     * some programs use this as the certMd5 of certificate
     */
    val certBase64Md5: String,
    /**
     * use md5 to calculate certificate's certMd5.
     */
    val certMd5: String
) {

    override fun toString(): String {
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return "CertificateMeta{signAlgorithm=" + signAlgorithm + ", " +
                "certBase64Md5=" + certBase64Md5 + ", " +
                "startDate=" + df.format(startDate) + ", " + "endDate=" + df.format(endDate) + "}"
    }
}