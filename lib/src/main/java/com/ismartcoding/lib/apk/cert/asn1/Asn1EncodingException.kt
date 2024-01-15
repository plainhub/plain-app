package com.ismartcoding.lib.apk.cert.asn1

/**
 * Indicates that an ASN.1 structure could not be encoded.
 */
class Asn1EncodingException : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    companion object {
        private const val serialVersionUID = 1L
    }
}