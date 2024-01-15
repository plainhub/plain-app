package com.ismartcoding.lib.apk.cert.asn1

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Asn1Field(
    /**
     * Index used to order fields in a container. Required for fields of SEQUENCE containers.
     */
    val index: Int = 0, val cls: Asn1TagClass = Asn1TagClass.AUTOMATIC, val type: Asn1Type,
    /**
     * Tagging mode. Default: NORMAL.
     */
    val tagging: Asn1Tagging = Asn1Tagging.NORMAL,
    /**
     * Tag number. Required when IMPLICIT and EXPLICIT tagging mode is used.
     */
    val tagNumber: Int = -1,
    /**
     * `true` if this field is optional. Ignored for fields of CHOICE containers.
     */
    val optional: Boolean = false,
    /**
     * Type of elements. Used only for SET_OF or SEQUENCE_OF.
     */
    val elementType: Asn1Type = Asn1Type.ANY
)