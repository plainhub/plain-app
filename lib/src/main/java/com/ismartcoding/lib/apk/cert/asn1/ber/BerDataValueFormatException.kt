package com.ismartcoding.lib.apk.cert.asn1.ber

/**
 * Indicates that an ASN.1 data value being read could not be decoded using
 * Basic Encoding Rules (BER).
 */
class BerDataValueFormatException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}