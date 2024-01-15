package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1Tagging
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type
import java.nio.ByteBuffer

/**
 * PKCS #7 `EncapsulatedContentInfo` as specified in RFC 5652.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class EncapsulatedContentInfo {
    @Asn1Field(index = 0, type = Asn1Type.OBJECT_IDENTIFIER)
    var contentType: String? = null

    @Asn1Field(
        index = 1,
        type = Asn1Type.OCTET_STRING,
        tagging = Asn1Tagging.EXPLICIT,
        tagNumber = 0,
        optional = true
    )
    var content: ByteBuffer? = null

    constructor()
    constructor(contentTypeOid: String?) {
        contentType = contentTypeOid
    }
}