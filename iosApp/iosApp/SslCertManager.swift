import Foundation
import Security
import CryptoKit
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
              let identity = first[kSecImportItemIdentity as String] as? SecIdentity else {
            throw Self.error("No private key found in the certificate file")
        }
        return try storeIdentity(identity)
    }

    func replaceCertWithPem(certPem: String, keyPem: String) throws -> KotlinByteArray {
        guard let certDer = Self.pemDERBlock(certPem, type: "CERTIFICATE"),
              let cert = SecCertificateCreateWithData(nil, certDer as CFData) else {
            throw Self.error("Invalid certificate file")
        }
        // SecItemImport handles all common PEM key encodings (PKCS#8, PKCS#1 RSA,
        // SEC1 EC) and adds the key to the default keychain so it can back a SecIdentity.
        var format: SecExternalFormat = .formatPEMSequence
        var itemType: SecItemClass = kSecItemTypeAggregate
        var importedItems: CFArray?
        let importStatus = SecItemImport(Data(keyPem.utf8) as CFData, nil, &format, &itemType, [], nil, nil, &importedItems)
        guard importStatus == errSecSuccess, let importedItems = importedItems as? [CFTypeRef] else {
            throw Self.error("Invalid private key")
        }
        var key: SecKey?
        for item in importedItems where CFGetTypeID(item) == SecKeyGetTypeID() {
            key = (item as! SecKey)
            break
        }
        guard let key else {
            throw Self.error("No private key found in the PEM file")
        }
        var identity: SecIdentity?
        let identityStatus = SecIdentityCreateWithCertificate(nil, cert, &identity)
        guard identityStatus == errSecSuccess, let identity else {
            throw Self.error("Certificate and private key do not match")
        }
        return try storeIdentity(identity)
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

    /// Extract the certificate + private key from a `SecIdentity` (produced by
    /// `SecPKCS12Import` or `SecIdentityCreateWithCertificate`) and persist them
    /// in the Keychain in the same slots used by the self-signed flow.
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
        // NIOSSL loads private keys from DER as PKCS#8 (`d2i_AutoPrivateKey` also
        // accepts PKCS#1/SEC1), so export the key in PKCS#8 format.
        var pkcs8Out: CFData?
        let exportStatus = SecItemExport(keyRef, .formatPKCS8, [], nil, &pkcs8Out)
        guard exportStatus == errSecSuccess, let pkcs8Data = pkcs8Out as Data, !pkcs8Data.isEmpty else {
            throw Self.error("Failed to export the private key")
        }

        let signature = try Self.extractSignature(fromCertDer: certDer)
        saveToKeychain(key: certKeychainKey, data: certDer)
        saveToKeychain(key: privateKeyKeychainKey, data: pkcs8Data)
        saveToKeychain(key: sigKeychainKey, data: signature)
        return Self.toKotlinByteArray(signature)
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

    /// Locate the first PEM block of [type] (e.g. "CERTIFICATE") and return its DER bytes.
    private static func pemDERBlock(_ pem: String, type: String) -> Data? {
        var inBlock = false
        var base64 = ""
        for rawLine in pem.components(separatedBy: .newlines) {
            let line = rawLine.trimmingCharacters(in: .whitespaces)
            if line.hasPrefix("-----BEGIN ") && line.contains(type) {
                inBlock = true
                continue
            }
            if line.hasPrefix("-----END ") {
                inBlock = false
                break
            }
            if inBlock {
                base64 += line
            }
        }
        guard !base64.isEmpty else { return nil }
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
