package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1Tagging
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type
import java.nio.ByteBuffer

/**
 * PKCS #7 `SignerIdentifier` as specified in RFC 5652.
 */
@Asn1Class(type = Asn1Type.CHOICE)
class SignerIdentifier {
    @Asn1Field(type = Asn1Type.SEQUENCE)
    var issuerAndSerialNumber: IssuerAndSerialNumber? = null

    @Asn1Field(type = Asn1Type.OCTET_STRING, tagging = Asn1Tagging.IMPLICIT, tagNumber = 0)
    var subjectKeyIdentifier: ByteBuffer? = null

    constructor()
    constructor(issuerAndSerialNumber: IssuerAndSerialNumber?) {
        this.issuerAndSerialNumber = issuerAndSerialNumber
    }
}