package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1OpaqueObject
import com.ismartcoding.lib.apk.cert.asn1.Asn1Tagging
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type

/**
 * PKCS #7 `ContentInfo` as specified in RFC 5652.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class ContentInfo {
    @Asn1Field(index = 1, type = Asn1Type.OBJECT_IDENTIFIER)
    var contentType: String? = null

    @Asn1Field(index = 2, type = Asn1Type.ANY, tagging = Asn1Tagging.EXPLICIT, tagNumber = 0)
    var content: Asn1OpaqueObject? = null
}