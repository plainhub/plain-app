@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.lib.crypto.sha256
import kotlinx.cinterop.readBytes
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.*
import platform.Security.SecCertificateCopyData
import platform.Security.SecTrustGetCertificateAtIndex

internal fun peerResourceCredential(challenge: NSURLAuthenticationChallenge, host: String?, fingerprint: String): NSURLCredential? {
    val space = challenge.protectionSpace
    if (space.authenticationMethod != NSURLAuthenticationMethodServerTrust || space.host != host) return null
    val trust = space.serverTrust ?: return null
    val leaf = SecTrustGetCertificateAtIndex(trust, 0) ?: return null
    val data = SecCertificateCopyData(leaf) ?: return null
    val digest = try {
        CFDataGetBytePtr(data)?.readBytes(CFDataGetLength(data).toInt())?.let(::sha256)
    } finally { CFRelease(data) }
    val encoded = digest?.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    return if (encoded == fingerprint) NSURLCredential.credentialForTrust(trust) else null
}
