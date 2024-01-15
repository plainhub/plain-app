package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1OpaqueObject
import com.ismartcoding.lib.apk.cert.asn1.Asn1Tagging
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type
import java.nio.ByteBuffer

/**
 * PKCS #7 `SignerInfo` as specified in RFC 5652.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class SignerInfo {
    @Asn1Field(index = 0, type = Asn1Type.INTEGER)
    var version = 0

    @Asn1Field(index = 1, type = Asn1Type.CHOICE)
    var sid: SignerIdentifier? = null

    @Asn1Field(index = 2, type = Asn1Type.SEQUENCE)
    var digestAlgorithm: AlgorithmIdentifier? = null

    @Asn1Field(
        index = 3,
        type = Asn1Type.SET_OF,
        tagging = Asn1Tagging.IMPLICIT,
        tagNumber = 0,
        optional = true
    )
    var signedAttrs: Asn1OpaqueObject? = null

    @Asn1Field(index = 4, type = Asn1Type.SEQUENCE)
    var signatureAlgorithm: AlgorithmIdentifier? = null

    @Asn1Field(index = 5, type = Asn1Type.OCTET_STRING)
    var signature: ByteBuffer? = null

    @Asn1Field(
        index = 6,
        type = Asn1Type.SET_OF,
        tagging = Asn1Tagging.IMPLICIT,
        tagNumber = 1,
        optional = true
    )
    var unsignedAttrs: List<Attribute>? = null
}