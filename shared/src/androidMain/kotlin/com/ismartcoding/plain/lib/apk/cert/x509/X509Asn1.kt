package com.ismartcoding.plain.lib.apk.cert.x509

import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1Class
import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1Field
import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1OpaqueObject
import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1Tagging
import com.ismartcoding.plain.lib.apk.cert.asn1.Asn1Type
import java.math.BigInteger

object X509Oid {
    const val SHA256_WITH_ECDSA = "1.2.840.10045.4.3.2"
    const val EC_PUBLIC_KEY = "1.2.840.10045.2.1"
    const val CURVE_P256 = "1.2.840.10045.3.1.7"
    const val AT_COMMON_NAME = "2.5.4.3"
    const val AT_ORGANIZATION = "2.5.4.10"

    /** DER bytes of ASN.1 NULL, used as the parameters of an ECDSA signature algorithm. */
    val NULL_DER = byteArrayOf(0x05, 0x00)
}

/**
 * `AttributeTypeAndValue ::= SEQUENCE { type OBJECT IDENTIFIER, value UTF8String }`
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class AttributeTypeAndValue {
    @Asn1Field(index = 0, type = Asn1Type.OBJECT_IDENTIFIER)
    var type: String? = null

    @Asn1Field(index = 1, type = Asn1Type.UTF8_STRING)
    var value: String? = null

    constructor()
    constructor(type: String?, value: String?) {
        this.type = type
        this.value = value
    }
}

/**
 * X.500 name used for both issuer and subject. A single relative distinguished
 * name (a SET of attribute type/value pairs) — faithful to the reference BKS
 * certificate built as `CN=<name>, O=<name>`.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class X500Name {
    @Asn1Field(index = 0, type = Asn1Type.SET_OF)
    var attributes: List<AttributeTypeAndValue>? = null

    constructor()
    constructor(commonName: String) {
        attributes = listOf(
            AttributeTypeAndValue(X509Oid.AT_COMMON_NAME, commonName),
            AttributeTypeAndValue(X509Oid.AT_ORGANIZATION, commonName),
        )
    }
}

/**
 * `Validity ::= SEQUENCE { notBefore Time, notAfter Time }`.
 * Both fields use GeneralizedTime (`yyyyMMddHHmmssZ`) to avoid the UTCTime 2050 boundary.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class Validity {
    @Asn1Field(index = 0, type = Asn1Type.GENERALIZED_TIME)
    var notBefore: String? = null

    @Asn1Field(index = 1, type = Asn1Type.GENERALIZED_TIME)
    var notAfter: String? = null

    constructor()
    constructor(notBefore: String?, notAfter: String?) {
        this.notBefore = notBefore
        this.notAfter = notAfter
    }
}

/**
 * `AlgorithmIdentifier ::= SEQUENCE { algorithm OBJECT IDENTIFIER, parameters ANY OPTIONAL }`
 * as used by the signatureAlgorithm of a certificate: algorithm `sha256WithECDSA`, parameters NULL.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class EcdsaSignatureAlgorithm {
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

/**
 * `AlgorithmIdentifier ::= SEQUENCE { algorithm OBJECT IDENTIFIER, parameters OBJECT IDENTIFIER }`
 * for an EC named-curve public key: algorithm `ecPublicKey`, parameters the P-256 curve OID.
 * The curve is a real OID value (not an opaque blob).
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class EcPublicKeyAlgorithm {
    @Asn1Field(index = 0, type = Asn1Type.OBJECT_IDENTIFIER)
    var algorithm: String? = null

    @Asn1Field(index = 1, type = Asn1Type.OBJECT_IDENTIFIER)
    var curve: String? = null

    constructor()
    constructor(algorithmOid: String?, curveOid: String?) {
        algorithm = algorithmOid
        curve = curveOid
    }
}

/**
 * `SubjectPublicKeyInfo ::= SEQUENCE { algorithm AlgorithmIdentifier, subjectPublicKey BIT STRING }`.
 * Wraps an EC P-256 named-curve key: algorithm `ecPublicKey`, curve P-256, and the public key as
 * an uncompressed point `0x04||X||Y`.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class SubjectPublicKeyInfo {
    @Asn1Field(index = 0, type = Asn1Type.SEQUENCE)
    var algorithm: EcPublicKeyAlgorithm? = null

    @Asn1Field(index = 1, type = Asn1Type.BIT_STRING)
    var subjectPublicKey: ByteArray? = null

    constructor()
    constructor(algorithm: EcPublicKeyAlgorithm?, subjectPublicKey: ByteArray?) {
        this.algorithm = algorithm
        this.subjectPublicKey = subjectPublicKey
    }
}

/**
 * `TBSCertificate ::= SEQUENCE { ... }`. Optional fields are emitter-only here:
 * every field the generator needs is required. Version is v3 (`[0] EXPLICIT Version`).
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class TbsCertificate {
    @Asn1Field(index = 0, type = Asn1Type.INTEGER, tagging = Asn1Tagging.EXPLICIT, tagNumber = 0)
    var version: Int? = null

    @Asn1Field(index = 1, type = Asn1Type.INTEGER)
    var serialNumber: BigInteger? = null

    @Asn1Field(index = 2, type = Asn1Type.SEQUENCE)
    var signature: EcdsaSignatureAlgorithm? = null

    @Asn1Field(index = 3, type = Asn1Type.SEQUENCE)
    var issuer: X500Name? = null

    @Asn1Field(index = 4, type = Asn1Type.SEQUENCE)
    var validity: Validity? = null

    @Asn1Field(index = 5, type = Asn1Type.SEQUENCE)
    var subject: X500Name? = null

    @Asn1Field(index = 6, type = Asn1Type.SEQUENCE)
    var subjectPublicKeyInfo: SubjectPublicKeyInfo? = null
}

/**
 * `Certificate ::= SEQUENCE { tbsCertificate TBSCertificate, signatureAlgorithm AlgorithmIdentifier,
 * signatureValue BIT STRING }`. The pre-signed TBS is embedded as an opaque blob so the signature
 * covers exactly the bytes handed to the signer.
 */
@Asn1Class(type = Asn1Type.SEQUENCE)
class X509CertificateStructure {
    @Asn1Field(index = 0, type = Asn1Type.ANY)
    var tbsCertificate: Asn1OpaqueObject? = null

    @Asn1Field(index = 1, type = Asn1Type.SEQUENCE)
    var signatureAlgorithm: EcdsaSignatureAlgorithm? = null

    @Asn1Field(index = 2, type = Asn1Type.BIT_STRING)
    var signatureValue: ByteArray? = null

    constructor()
    constructor(tbsCertificate: Asn1OpaqueObject?, signatureAlgorithm: EcdsaSignatureAlgorithm?, signatureValue: ByteArray?) {
        this.tbsCertificate = tbsCertificate
        this.signatureAlgorithm = signatureAlgorithm
        this.signatureValue = signatureValue
    }
}