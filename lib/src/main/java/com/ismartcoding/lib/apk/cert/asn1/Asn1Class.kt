package com.ismartcoding.lib.apk.cert.asn1

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Asn1Class(val type: Asn1Type)