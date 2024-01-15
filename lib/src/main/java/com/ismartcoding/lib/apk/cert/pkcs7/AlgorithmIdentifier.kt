package com.ismartcoding.lib.apk.cert.pkcs7

import com.ismartcoding.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.lib.apk.cert.asn1.Asn1OpaqueObject
import com.ismartcoding.lib.apk.cert.asn1.Asn1Type

@Asn1Class(type = Asn1Type.SEQUENCE)
class AlgorithmIdentifier {
    @Asn1Field(index = 0, type = Asn1Type.OBJECT_IDENTIFIER)
    var algorithm: String? = null

    @Asn1Field(index = 1, type = Asn1Type.ANY, optional = true)
    var parameters: Asn1OpaqueObject? = null

    constructor()
    constructor(algorithmOid: String?, parameters: Asn1OpaqueObject?) {
        algorithm = algorithmOid
        this.parameters = parameters
    }
}