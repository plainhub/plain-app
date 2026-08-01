import Foundation
import CryptoKit
import PlainShared

/// Swift implementation of `IosSslCertProvider`.
///
/// Generates an EC P-256 self-signed X.509 v3 certificate, persists the DER
/// encoding and its ECDSA signature in the iOS Keychain so the certificate
/// survives app restarts. This prevents the certificate fingerprint from
/// changing on every launch (which would force web clients to re-pair).
///
/// The certificate is regenerated only when the user explicitly presses
/// "Regenerate SSL" in the Web Security page (Kotlin calls `regenerateCert`).
final class SslCertManager: NSObject, IosSslCertProvider {

    private let certKeychainKey = "com.ismartcoding.plain.ssl.cert.der"
    private let sigKeychainKey = "com.ismartcoding.plain.ssl.cert.sig"

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
        let (_, signature) = generateAndStoreCert()
        return Self.toKotlinByteArray(signature)
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

    // MARK: - Certificate generation

    private func generateAndStoreCert() -> (Data, Data) {
        let (certDer, signature) = Self.generateSelfSignedCert()
        saveToKeychain(key: certKeychainKey, data: certDer)
        saveToKeychain(key: sigKeychainKey, data: signature)
        return (certDer, signature)
    }

    /// Build a minimal self-signed X.509 v3 certificate using EC P-256.
    ///
    /// The ASN.1 structure is:
    /// ```
    /// Certificate ::= SEQUENCE {
    ///     tbsCertificate       TBSCertificate,
    ///     signatureAlgorithm   AlgorithmIdentifier,
    ///     signatureValue       BIT STRING
    /// }
    /// ```
    private static func generateSelfSignedCert() -> (Data, Data) {
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

        return (cert, sigDer)
    }

    /// Build the TBSCertificate (To-Be-Signed) DER structure.
    private static func buildTbsCertificate(
        publicKey: P256.Signing.PublicKey,
        cn: String
    ) -> Data {
        // version [0] EXPLICIT INTEGER 2 (v3)
        let version = DerEncoder.explicit(tag: 0xA0, inner: DerEncoder.integer(2))

        // serialNumber INTEGER
        let serial = DerEncoder.integer(UUID().uuidString.hashValue)

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
        for i in 0..<temp.count {
            if i < temp.count - 1 {
                bytes.append(temp[i] | 0x80)
            } else {
                bytes.append(temp[i])
            }
        }
    }
}
