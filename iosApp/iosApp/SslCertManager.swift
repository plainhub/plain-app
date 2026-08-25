import Foundation
import Security
import CryptoKit
import NIOCore
import NIOSSL
import PlainShared

/// Swift implementation of `IosSslCertProvider`.
///
/// Generates an EC P-256 self-signed X.509 v3 certificate, persists the DER
/// encoding, its ECDSA signature and the private key in the iOS Keychain so
/// the certificate survives app restarts. This prevents the certificate
/// fingerprint from changing on every launch (which would force web clients
/// to re-pair) and lets the HTTPS connector reuse the same key material.
///
/// The certificate is regenerated only when the user explicitly presses
/// "Regenerate SSL" in the Web Security page (Kotlin calls `regenerateCert`).
///
/// User-provided certificates (PKCS#12 bundles or PEM cert+key pairs) are
/// imported via Security.framework and stored in the same Keychain slots
/// (cert DER + PKCS#8 private key DER), so `makeTLSConfiguration` works
/// unchanged.
final class SslCertManager: NSObject, IosSslCertProvider {

    private let certKeychainKey = "com.ismartcoding.plain.ssl.cert.der"
    private let sigKeychainKey = "com.ismartcoding.plain.ssl.cert.sig"
    private let privateKeyKeychainKey = "com.ismartcoding.plain.ssl.private.key"

    // MARK: - IosSslCertProvider

    func getCertSignatureBytes() -> KotlinByteArray {
        if let sig = loadFromKeychain(key: sigKeychainKey) {
            return Self.toKotlinByteArray(sig)
        }
        let (_, signature) = generateAndStoreCert()
        return Self.toKotlinByteArray(signature)
    }

    func regenerateCert() -> KotlinByteArray {
        deleteFromKeychain(key: certKeychainKey)
        deleteFromKeychain(key: sigKeychainKey)
        deleteFromKeychain(key: privateKeyKeychainKey)
        let (_, signature) = generateAndStoreCert()
        return Self.toKotlinByteArray(signature)
    }

    func replaceCertWithPkcs12(p12Data: KotlinByteArray, password: String) throws -> KotlinByteArray {
        let data = p12Data.toNSData() as Data
        guard !data.isEmpty else {
            throw Self.error("Invalid certificate file")
        }
        let options: [String: Any] = [kSecImportExportPassphrase as String: password]
        var rawItems: CFArray?
        let status = SecPKCS12Import(data as CFData, options as CFDictionary, &rawItems)
        guard status == errSecSuccess else {
            throw Self.error(status == errSecAuthFailed ? "Wrong password" : "Invalid certificate file")
        }
        guard let items = rawItems as? [[String: Any]],
              let first = items.first,
              let identity = first[kSecImportItemIdentity as String] as! SecIdentity? else {
            throw Self.error("No private key found in the certificate file")
        }
        return try storeIdentity(identity)
    }

    func replaceCertWithPem(certPem: String, keyPem: String) throws -> KotlinByteArray {
        guard let certDer = Self.pemDERBlock(certPem, type: "CERTIFICATE"),
              SecCertificateCreateWithData(nil, certDer as CFData) != nil else {
            throw Self.error("Invalid certificate file")
        }
        // NIOSSL's `d2i_AutoPrivateKey` accepts PKCS#8, SEC1 EC and PKCS#1 RSA
        // DER, so store the raw decoded key block directly (no SecKey needed).
        let keyDer = try Self.pemPrivateKeyDER(keyPem)
        return try persistCredentials(certDer: certDer, keyDer: keyDer)
    }

    /// Build a server TLS configuration from the persisted self-signed
    /// certificate and its private key. Generates + persists a fresh pair on
    /// first call (when nothing is stored yet).
    func makeTLSConfiguration() throws -> TLSConfiguration {
        var certData = loadFromKeychain(key: certKeychainKey)
        var keyData = loadFromKeychain(key: privateKeyKeychainKey)
        if certData == nil || keyData == nil {
            _ = generateAndStoreCert()
            certData = loadFromKeychain(key: certKeychainKey)
            keyData = loadFromKeychain(key: privateKeyKeychainKey)
        }
        guard let certData, let keyData else {
            throw NSError(domain: "SslCertManager", code: -1,
                          userInfo: [NSLocalizedDescriptionKey: "Failed to load TLS credentials"])
        }
        let cert = try NIOSSLCertificate(bytes: Array(certData), format: .der)
        let key = try NIOSSLPrivateKey(bytes: Array(keyData), format: .der)

        return TLSConfiguration.makeServerConfiguration(
            certificateChain: [.certificate(cert)],
            privateKey: .privateKey(key)
        )
    }

    private static func toKotlinByteArray(_ data: Data) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(data.count))
        data.withUnsafeBytes { (ptr: UnsafeRawBufferPointer) in
            for i in 0..<data.count {
                array.set(index: Int32(i), value: Int8(bitPattern: ptr[i]))
            }
        }
        return array
    }

    // MARK: - User certificate import

    /// Persist [certDer] + [keyDer] in the Keychain and return the new
    /// certificate's signature bytes. Both inputs are DER that NIOSSL can load
    /// (`NIOSSLCertificate(bytes:format:.der)` / `NIOSSLPrivateKey(bytes:format:.der)`).
    private func persistCredentials(certDer: Data, keyDer: Data) throws -> KotlinByteArray {
        let signature = try Self.extractSignature(fromCertDer: certDer)
        saveToKeychain(key: certKeychainKey, data: certDer)
        saveToKeychain(key: privateKeyKeychainKey, data: keyDer)
        saveToKeychain(key: sigKeychainKey, data: signature)
        return Self.toKotlinByteArray(signature)
    }

    /// Extract the certificate + private key from a `SecIdentity` (produced by
    /// `SecPKCS12Import`) and persist them in the Keychain in the same slots
    /// used by the self-signed flow.
    private func storeIdentity(_ identity: SecIdentity) throws -> KotlinByteArray {
        var certRef: SecCertificate?
        guard SecIdentityCopyCertificate(identity, &certRef) == errSecSuccess, let certRef else {
            throw Self.error("No certificate found in the certificate file")
        }
        let certDer = SecCertificateCopyData(certRef) as Data

        var keyRef: SecKey?
        guard SecIdentityCopyPrivateKey(identity, &keyRef) == errSecSuccess, let keyRef else {
            throw Self.error("No private key found in the certificate file")
        }
        let keyDer = try exportPrivateKeyDer(keyRef)
        return try persistCredentials(certDer: certDer, keyDer: keyDer)
    }

    /// Export [key] as DER that NIOSSL can load. RSA keys are returned by the
    /// platform as PKCS#1 DER already. EC keys come back as a combined
    /// `0x04 || X || Y || scalar` blob (there is no public PKCS#8 export API on
    /// iOS), so it is re-wrapped into a SEC1 `ECPrivateKey` structure.
    private func exportPrivateKeyDer(_ key: SecKey) throws -> Data {
        var error: Unmanaged<CFError>?
        guard let external = SecKeyCopyExternalRepresentation(key, &error) as Data? else {
            throw Self.error("Failed to export the private key")
        }
        let attrs = SecKeyCopyAttributes(key) as? [String: Any]
        let keyType = attrs?[kSecAttrKeyType as String] as? String
        if keyType == kSecAttrKeyTypeRSA as String {
            return external // already PKCS#1 DER
        }
        // EC: 0x04 || X(32) || Y(32) || scalar(32) for P-256
        let bytes = [UInt8](external)
        guard bytes.count > 65, bytes[0] == 0x04 else {
            throw Self.error("Unsupported EC key representation")
        }
        let scalar = Data(bytes[65...])
        let point = external.prefix(65)
        let bits = (attrs?[kSecAttrKeySizeInBits as String] as? Int) ?? 0
        guard let curve = Self.ecCurveOID(bits: bits) else {
            throw Self.error("Unsupported EC key size")
        }
        // SEC1 ECPrivateKey ::= SEQUENCE { version, scalar, [0] curve, [1] publicPoint }
        return DerEncoder.sequence([
            DerEncoder.integer(1),
            DerEncoder.octetString(scalar),
            DerEncoder.explicit(tag: 0xA0, inner: DerEncoder.oid(curve)),
            DerEncoder.explicit(tag: 0xA1, inner: DerEncoder.bitString(Data(point))),
        ])
    }

    /// Object identifiers for the supported NIST EC curves, keyed by key size in bits.
    private static func ecCurveOID(bits: Int) -> [Int]? {
        switch bits {
        case 256: return [1, 2, 840, 10045, 3, 1, 7] // prime256v1
        case 384: return [1, 3, 132, 0, 34]           // secp384r1
        case 521: return [1, 3, 132, 0, 35]           // secp521r1
        default: return nil
        }
    }

    /// Extract the first supported private key PEM block (PKCS#8, SEC1 EC, or
    /// PKCS#1 RSA) as DER. Throws for encrypted/unsupported keys.
    private static func pemPrivateKeyDER(_ pem: String) throws -> Data {
        for type in ["PRIVATE KEY", "EC PRIVATE KEY", "RSA PRIVATE KEY"] {
            if let der = pemDERBlock(pem, type: type) {
                return der
            }
        }
        throw Self.error("Unsupported private key format")
    }

    /// Extract the `signatureValue` (BIT STRING) from a DER-encoded X.509
    /// certificate: `SEQUENCE { tbsCertificate, signatureAlgorithm, signatureValue }`.
    private static func extractSignature(fromCertDer der: Data) throws -> Data {
        var idx = 0
        let outer = try readTLV(der, at: &idx)
        guard outer.tag == 0x30 else { throw error("Invalid certificate file") }
        let content = outer.content
        var i = 0
        let tbs = try readTLV(content, at: &i)
        guard tbs.tag == 0x30 else { throw error("Invalid certificate file") }
        let sigAlg = try readTLV(content, at: &i)
        guard sigAlg.tag == 0x30 else { throw error("Invalid certificate file") }
        let sig = try readTLV(content, at: &i)
        guard sig.tag == 0x03, !sig.content.isEmpty else { throw error("Invalid certificate file") }
        // BIT STRING payload: first byte = number of unused bits, rest = signature bytes
        return sig.content.dropFirst()
    }

    private static func readTLV(_ data: Data, at index: inout Int) throws -> (tag: UInt8, content: Data) {
        guard index < data.count else { throw error("Invalid certificate file") }
        let tag = data[index]
        index += 1
        guard index < data.count else { throw error("Invalid certificate file") }
        let firstLen = data[index]
        index += 1
        var length = 0
        if firstLen < 0x80 {
            length = Int(firstLen)
        } else {
            let numBytes = Int(firstLen & 0x7F)
            guard numBytes > 0, numBytes <= 4, index + numBytes <= data.count else { throw error("Invalid certificate file") }
            for _ in 0..<numBytes {
                length = (length << 8) | Int(data[index])
                index += 1
            }
        }
        guard length >= 0, index + length <= data.count else { throw error("Invalid certificate file") }
        let content = data.subdata(in: index..<(index + length))
        index += length
        return (tag, content)
    }

    /// Locate the first PEM block with the exact marker [type] (e.g. "CERTIFICATE",
    /// "PRIVATE KEY") and return its DER bytes. Exact marker matching keeps
    /// "EC PRIVATE KEY" distinct from "PRIVATE KEY".
    private static func pemDERBlock(_ pem: String, type: String) -> Data? {
        let beginMarker = "-----BEGIN \(type)-----"
        let endMarker = "-----END \(type)-----"
        guard let begin = pem.range(of: beginMarker),
              let end = pem.range(of: endMarker), end.lowerBound > begin.upperBound else {
            return nil
        }
        let base64 = pem[begin.upperBound..<end.lowerBound]
            .components(separatedBy: .whitespacesAndNewlines)
            .joined()
        return Data(base64Encoded: base64)
    }

    private static func error(_ message: String) -> NSError {
        NSError(domain: "SslCertManager", code: -1,
                userInfo: [NSLocalizedDescriptionKey: message])
    }

    // MARK: - Certificate generation

    private func generateAndStoreCert() -> (Data, Data) {
        let (privateKeyDer, certDer, signature) = Self.generateSelfSignedCert()
        saveToKeychain(key: privateKeyKeychainKey, data: privateKeyDer)
        saveToKeychain(key: certKeychainKey, data: certDer)
        saveToKeychain(key: sigKeychainKey, data: signature)
        return (certDer, signature)
    }

    /// Build a minimal self-signed X.509 v3 certificate using EC P-256.
    ///
    /// Returns the PKCS#8 private key DER, the certificate DER and the raw
    /// ECDSA signature.
    ///
    /// The ASN.1 structure is:
    /// ```
    /// Certificate ::= SEQUENCE {
    ///     tbsCertificate       TBSCertificate,
    ///     signatureAlgorithm   AlgorithmIdentifier,
    ///     signatureValue       BIT STRING
    /// }
    /// ```
    private static func generateSelfSignedCert() -> (Data, Data, Data) {
        let privateKey = P256.Signing.PrivateKey()
        let publicKey = privateKey.publicKey
        let cn = "PlainApp"

        let tbs = buildTbsCertificate(publicKey: publicKey, cn: cn)
        let signature = try! privateKey.signature(for: tbs)
        let sigDer = signature.derRepresentation

        let algorithm = DerEncoder.sequence([
            DerEncoder.oid([1, 2, 840, 10045, 4, 3, 2]),
            DerEncoder.null()
        ])
        let bitString = DerEncoder.bitString(sigDer)
        let cert = DerEncoder.sequence([tbs, algorithm, bitString])

        return (privateKey.derRepresentation, cert, sigDer)
    }

    /// Build the TBSCertificate (To-Be-Signed) DER structure.
    private static func buildTbsCertificate(
        publicKey: P256.Signing.PublicKey,
        cn: String
    ) -> Data {
        // version [0] EXPLICIT INTEGER 2 (v3)
        let version = DerEncoder.explicit(tag: 0xA0, inner: DerEncoder.integer(2))

        // serialNumber INTEGER — DER INTEGER 必须非负，取随机正数
        let serial = DerEncoder.integer(Int.random(in: 1...Int.max))

        // signature AlgorithmIdentifier (ecdsa-with-SHA256)
        let sigAlg = DerEncoder.sequence([
            DerEncoder.oid([1, 2, 840, 10045, 4, 3, 2]),
            DerEncoder.null()
        ])

        // issuer = subject = CN=cn
        let cnOid: [UInt8] = [0x55, 0x04, 0x03]  // 2.5.4.3 commonName
        let name = DerEncoder.sequence([
            DerEncoder.set([
                DerEncoder.sequence([
                    DerEncoder.oid(bytes: cnOid),
                    DerEncoder.utf8String(cn)
                ])
            ])
        ])

        // validity
        let now = Date()
        let notBefore = DerEncoder.utcTime(now)
        let notAfter = DerEncoder.utcTime(now.addingTimeInterval(20 * 365 * 24 * 3600))
        let validity = DerEncoder.sequence([notBefore, notAfter])

        // SubjectPublicKeyInfo (EC P-256 uncompressed)
        let ecPubKeyOid: [UInt8] = [0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02, 0x01]  // 1.2.840.10045.2.1
        let prime256v1Oid: [UInt8] = [0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07]  // 1.2.840.10045.3.1.7
        let algId = DerEncoder.sequence([
            DerEncoder.oid(bytes: ecPubKeyOid),
            DerEncoder.oid(bytes: prime256v1Oid)
        ])
        let pubKeyBits = publicKey.x963Representation  // 65 bytes: 0x04 || X || Y
        let spki = DerEncoder.sequence([
            algId,
            DerEncoder.bitString(pubKeyBits)
        ])

        return DerEncoder.sequence([
            version, serial, sigAlg, name, validity, name, spki
        ])
    }

    // MARK: - Keychain helpers

    private func saveToKeychain(key: String, data: Data) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)

        var addQuery = query
        addQuery[kSecValueData as String] = data
        SecItemAdd(addQuery as CFDictionary, nil)
    }

    private func loadFromKeychain(key: String) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess else { return nil }
        return result as? Data
    }

    private func deleteFromKeychain(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
}

// MARK: - KotlinByteArray → Data conversion

private extension KotlinByteArray {
    func toNSData() -> Data {
        let count = Int(self.size)
        var bytes = [UInt8](repeating: 0, count: count)
        for i in 0..<count {
            bytes[i] = UInt8(bitPattern: self.get(index: Int32(i)))
        }
        return Data(bytes)
    }
}

// MARK: - Minimal ASN.1 DER encoder

enum DerEncoder {

    static func sequence(_ items: [Data]) -> Data {
        return encode(tag: 0x30, content: concatenate(items))
    }

    static func set(_ items: [Data]) -> Data {
        return encode(tag: 0x31, content: concatenate(items))
    }

    static func explicit(tag: UInt8, inner: Data) -> Data {
        return encode(tag: tag, content: inner)
    }

    static func integer(_ value: Int) -> Data {
        var bytes = [UInt8]()
        var v = value
        if v == 0 {
            bytes = [0]
        } else {
            while v > 0 {
                bytes.insert(UInt8(v & 0xFF), at: 0)
                v >>= 8
            }
            if bytes[0] & 0x80 != 0 {
                bytes.insert(0, at: 0)
            }
        }
        return encode(tag: 0x02, content: Data(bytes))
    }

    static func null() -> Data {
        return Data([0x05, 0x00])
    }

    static func octetString(_ data: Data) -> Data {
        return encode(tag: 0x04, content: data)
    }

    static func bitString(_ data: Data) -> Data {
        var content = Data([0x00])  // 0 unused bits
        content.append(data)
        return encode(tag: 0x03, content: content)
    }

    static func utf8String(_ s: String) -> Data {
        return encode(tag: 0x0C, content: Data(s.utf8))
    }

    static func utcTime(_ date: Date) -> Data {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyMMddHHmmss'Z'"
        formatter.timeZone = TimeZone(identifier: "UTC")
        let s = formatter.string(from: date)
        return encode(tag: 0x17, content: Data(s.utf8))
    }

    static func oid(_ arcs: [Int]) -> Data {
        var bytes = [UInt8]()
        if arcs.count >= 2 {
            bytes.append(UInt8(arcs[0] * 40 + arcs[1]))
        }
        for i in 2..<arcs.count {
            encodeBase128(arcs[i], into: &bytes)
        }
        return encode(tag: 0x06, content: Data(bytes))
    }

    static func oid(bytes: [UInt8]) -> Data {
        return encode(tag: 0x06, content: Data(bytes))
    }

    // MARK: - Private

    private static func encode(tag: UInt8, content: Data) -> Data {
        var result = Data([tag])
        result.append(encodeLength(content.count))
        result.append(content)
        return result
    }

    private static func encodeLength(_ length: Int) -> Data {
        if length < 0x80 {
            return Data([UInt8(length)])
        }
        var bytes = [UInt8]()
        var l = length
        while l > 0 {
            bytes.insert(UInt8(l & 0xFF), at: 0)
            l >>= 8
        }
        return Data([UInt8(0x80 | bytes.count)] + bytes)
    }

    private static func concatenate(_ items: [Data]) -> Data {
        var result = Data()
        for item in items {
            result.append(item)
        }
        return result
    }

    private static func encodeBase128(_ value: Int, into bytes: inout [UInt8]) {
        var v = value
        var temp = [UInt8]()
        if v == 0 {
            temp.append(0)
        } else {
            while v > 0 {
                temp.append(UInt8(v & 0x7F))
                v >>= 7
            }
        }
        // DER base-128 要求高位组在前，收集时是低位在前，需反转。
        temp.reverse()
        for i in 0..<temp.count {
            if i < temp.count - 1 {
                bytes.append(temp[i] | 0x80)
            } else {
                bytes.append(temp[i])
            }
        }
    }
}
