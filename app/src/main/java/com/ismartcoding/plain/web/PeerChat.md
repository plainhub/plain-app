# Peer-to-Peer Chat System

A secure, end-to-end encrypted peer-to-peer chat implementation with Ed25519 digital signatures and XChaCha20-Poly1305 encryption.

## Overview

This system enables secure communication between paired devices without relying on central servers. It uses a combination of cryptographic protocols to ensure message authenticity, integrity, and confidentiality.

## Architecture

### Core Components

- **PeerChatHelper**: Main entry point for sending peer messages
- **GraphQLClient**: Handles GraphQL request construction and signature generation
- **PeerGraphQL**: Server-side GraphQL resolver with signature verification
- **PeerSignatureHelper**: Ed25519 signature operations

### Security Stack

```
Application Layer    →  GraphQL mutations/queries
Signature Layer      →  Ed25519 digital signatures  
Encryption Layer     →  XChaCha20-Poly1305 AEAD
Transport Layer      →  HTTPS with custom certificates
```

## Message Flow

```
PeerChatHelper.sendMessageToPeerAsync()
    ↓
GraphQLClient.createChatItem(content, timestamp)
    ↓  
executeRequest(needSignature = true)
    ↓
1. Build GraphQL JSON
2. Generate signature (timestamp + GraphQL JSON)
3. Assemble request (timestamp|signature|GraphQL_JSON)
4. Encrypt with XChaCha20-Poly1305
5. Send HTTPS request
    ↓
Server receives and processes:
1. Decrypt with XChaCha20-Poly1305
2. Parse timestamp|signature|GraphQL_JSON
3. Verify Ed25519 signature
4. Execute GraphQL if valid
5. Encrypt and return response
```

## Security Features

### 🔐 End-to-End Encryption
- **Algorithm**: XChaCha20-Poly1305 AEAD
- **Key Exchange**: ECDH (secp256r1) during pairing
- **Key Derivation**: SHA-256 hash of ECDH shared secret

### ✍️ Digital Signatures
- **Algorithm**: Ed25519 (Curve25519)
- **Purpose**: Message authenticity and integrity
- **Scope**: Covers timestamp + GraphQL JSON

### 🛡️ Anti-Replay Protection
- **Mechanism**: Timestamp validation (5-minute window)
- **Coverage**: All signed messages
- **Enforcement**: Both client and server side

### 🔒 Data Protection
- **In Transit**: HTTPS + ChaCha20 encryption
- **At Rest**: Local database encryption
- **Headers**: No sensitive data in HTTP headers

## Usage Example

```kotlin
// Send a text message to a paired peer
suspend fun sendMessage(peerId: String, messageText: String) {
    val content = DMessageContent().apply {
        type = DMessageType.TEXT.value
        value = messageText
    }
    
    val success = PeerChatHelper.sendMessageToPeerAsync(peerId, content)
    if (success) {
        println("Message sent successfully")
    } else {
        println("Failed to send message")
    }
}
```

## Technical Specifications

### Message Format

**Before Encryption:**
```
timestamp|signature|GraphQL_JSON
```

**GraphQL Structure:**
```json
{
  "query": "mutation CreateChatItem($content: String!) { createChatItem(content: $content) { id fromId toId createdAt } }",
  "variables": {
    "content": "{\"type\":\"text\",\"value\":\"Hello World\"}"
  }
}
```

**Signature Data:**
```
timestampGraphQL_JSON
```

### Cryptographic Details

#### Key Generation
```kotlin
// ECDH key pair for encryption
val keyPair = CryptoHelper.generateECDHKeyPair() // secp256r1

// Ed25519 key pair for signatures  
val sigKeyPair = CryptoHelper.generateEd25519KeyPair() // Ed25519
```

#### Encryption Process
```kotlin
// Derive shared secret
val sharedKey = CryptoHelper.computeECDHSharedKey(privateKey, peerPublicKey)

// Encrypt message
val ciphertext = CryptoHelper.chaCha20Encrypt(sharedKey, plaintext)
```

#### Signature Process
```kotlin
// Create signature data
val signatureData = "$timestamp$graphqlJson"

// Sign with Ed25519
val signature = PeerSignatureHelper.signMessageForPeer(signatureData)
```

## Error Handling

### Common Error Cases

1. **Peer Not Found**: Returns false immediately
2. **Peer Not Paired**: Checks pairing status before sending
3. **Signature Failure**: Aborts if signature generation fails
4. **Encryption Failure**: Handled by HttpClientManager
5. **Network Errors**: Propagated to caller with logging
6. **Timestamp Validation**: 5-minute window enforcement
7. **Signature Verification**: Ed25519 verification on server

### Logging

All operations include comprehensive logging:
- Debug: Successful operations and data flow
- Error: Failures with detailed error messages
- Security: Signature verification results

## Performance Considerations

### Optimizations
- **Signature Generation**: Unified in GraphQLClient.executeRequest
- **JSON Construction**: Single construction path
- **Error Handling**: Early validation and fast failure
- **Connection Reuse**: HTTP client connection pooling

### Scalability
- **Stateless Design**: No server-side session management
- **Async Operations**: Non-blocking I/O throughout
- **Resource Cleanup**: Automatic connection management

## Dependencies

### Core Libraries
- **Google Tink**: Ed25519 cryptographic operations
- **OkHttp**: HTTP client with custom SSL
- **KGraphQL**: GraphQL server implementation
- **Ktor**: HTTP server framework

### Platform Requirements
- **Android API**: 21+ (Android 5.0+)
- **Kotlin**: 1.8+
- **TLS**: 1.2+ for HTTPS transport

## Configuration

### Security Parameters
```kotlin
// Timestamp validation window
private const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

// HTTP client timeout
val httpClient = HttpClientManager.createCryptoHttpClient(peer.key, 10)

// Signature algorithms
Ed25519 (signing), XChaCha20-Poly1305 (encryption), ECDH secp256r1 (key exchange)
```

## Troubleshooting

### Common Issues

1. **Signature Verification Failed**
   - Check peer pairing status
   - Verify timestamp synchronization
   - Ensure Ed25519 keys are properly stored

2. **Connection Refused**
   - Verify peer IP and port
   - Check network connectivity
   - Confirm HTTPS certificate trust

3. **Decryption Failed**
   - Validate ECDH shared key
   - Check XChaCha20 key derivation
   - Verify encryption/decryption key consistency

### Debug Mode

Enable detailed logging:
```kotlin
LogCat.d("Message signed successfully for peer $peerId")
LogCat.d("GraphQL request with timestamp and signature: $finalRequestString")
LogCat.d("Signature verified successfully for peer $clientId")
```

## Future Enhancements

- **Group Chat**: Multi-peer encrypted conversations
- **File Transfer**: Secure peer-to-peer file sharing  
- **Message Reactions**: Emoji reactions with signatures
- **Read Receipts**: Delivery and read confirmations
- **Voice Messages**: Audio message support 