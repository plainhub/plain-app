package com.ismartcoding.lib.apk.cert.asn1

import java.nio.ByteBuffer

/**
 * Opaque holder of encoded ASN.1 stuff.
 */
class Asn1OpaqueObject {
    private val mEncoded: ByteBuffer

    constructor(encoded: ByteBuffer?) {
        mEncoded = encoded!!.slice()
    }

    constructor(encoded: ByteArray?) {
        mEncoded = ByteBuffer.wrap(encoded)
    }

    val encoded: ByteBuffer
        get() = mEncoded.slice()
}