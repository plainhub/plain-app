package com.ismartcoding.lib.apk.cert.asn1

/**
 * Indicates that input could not be decoded into intended ASN.1 structure.
 */
open class Asn1DecodingException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}