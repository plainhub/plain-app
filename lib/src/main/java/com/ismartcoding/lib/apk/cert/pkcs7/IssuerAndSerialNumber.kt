package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1OpaqueObject
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type
import java.math.BigInteger

/**
 * PKCS #7 `IssuerAndSerialNumber` as specified in RFC 5652.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class IssuerAndSerialNumber {
    @Asn1Field(index = 0, type = Asn1Type.ANY)
    var issuer: Asn1OpaqueObject? = null

    @Asn1Field(index = 1, type = Asn1Type.INTEGER)
    var certificateSerialNumber: BigInteger? = null

    constructor()
    constructor(issuer: Asn1OpaqueObject?, certificateSerialNumber: BigInteger?) {
        this.issuer = issuer
        this.certificateSerialNumber = certificateSerialNumber
    }
}