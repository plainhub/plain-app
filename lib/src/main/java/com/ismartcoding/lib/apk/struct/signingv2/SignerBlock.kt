package com.ismartcoding.lib.apk.struct.signingv2

import java.security.cert.X509Certificate

class SignerBlock(
    val digests: List<Digest>,
    val certificates: List<X509Certificate>,
    val signatures: List<Signature>
)