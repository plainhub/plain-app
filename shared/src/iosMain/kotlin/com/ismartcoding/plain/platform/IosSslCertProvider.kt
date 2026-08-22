package com.ismartcoding.plain.platform

/**
 * Swift-implemented self-signed SSL certificate provider.
 *
 * Generates an EC P-256 self-signed X.509 certificate, persists it in the
 * iOS Keychain so it survives app restarts (avoiding cert fingerprint
 * changes that would force web clients to re-pair), and exposes the
 * certificate's raw signature bytes for [getSSLSignature].
 *
 * Swift registers a single instance via [IosPlatformRegistry.setSslCertProvider]
 * at app startup.
 */
interface IosSslCertProvider {
    /**
     * Return the raw ECDSA signature bytes of the stored self-signed
     * certificate. Generates and persists a new certificate on first call.
     */
    fun getCertSignatureBytes(): ByteArray

    /**
     * Delete the existing certificate (if any) and generate + persist a
     * fresh one. Returns the new certificate's signature bytes.
     */
    fun regenerateCert(): ByteArray

    /**
     * Replace the stored certificate with the user-provided PKCS#12 bundle
     * ([p12Data] raw file bytes, [password] bundle password). Returns the new
     * certificate's signature bytes.
     *
     * @throws Exception when the bundle can't be parsed, the password is wrong,
     *         or no private key is found.
     */
    fun replaceCertWithPkcs12(p12Data: ByteArray, password: String): ByteArray

    /**
     * Replace the stored certificate with the user-provided PEM certificate
     * ([certPem]) and private key ([keyPem]). Returns the new certificate's
     * signature bytes.
     *
     * @throws Exception when either PEM file is malformed or they don't match.
     */
    fun replaceCertWithPem(certPem: String, keyPem: String): ByteArray
}
