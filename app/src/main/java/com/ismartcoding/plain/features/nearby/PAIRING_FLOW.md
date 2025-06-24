# Nearby Device Pairing Flow

This document describes how devices discover and pair with each other securely.

## Core Components

- **`NearbyDiscoverManager`** - Device discovery and message routing
- **`NearbyPairManager`** - Pairing process and key exchange
- **`NearbyViewModel`** - UI state management

## Message Types

- `DISCOVER` / `DISCOVER_REPLY` - Find nearby devices
- `PAIR_REQUEST` / `PAIR_RESPONSE` - Pairing negotiation
- `PAIR_CANCEL` - Cancel pairing process

## Discovery Flow

1. User clicks "Discover nearby devices"
2. Send UDP multicast every 5 seconds on port 52352
3. Other devices reply with their info (name, IP, type)
4. Update UI with discovered devices

## Pairing Flow

### 1. Initiate Pairing (Device A)
```
User clicks "Pair" → Generate ECDH keys → Send PAIR_REQUEST
```

### 2. Respond to Pairing (Device B)
```
Receive PAIR_REQUEST → Show dialog → User Allow/Deny → Send PAIR_RESPONSE
```

### 3. Complete Pairing
**If Accepted:**
- Both devices compute shared AES key using ECDH
- Store peer info in database
- Pairing success

**If Rejected:**
- Send rejection response
- Clean up session

## Cancellation Flow

**User cancels:**
1. Send `PAIR_CANCEL` to other device
2. Other device closes pairing dialog
3. Clean up sessions on both sides

## Security

- **ECDH Key Exchange** - Secure key agreement
- **AES Encryption** - For future communications
- **Session Cleanup** - No leaked cryptographic data

## Network Protocol

- **Discovery:** UDP multicast broadcast
- **Pairing:** UDP unicast point-to-point
- **Format:** JSON messages with type prefixes (`$DISCOVER$`, `$PAIR_REQUEST$`, etc.)

## Data Storage

Paired devices stored with:
- Device ID, name, IP address
- Shared AES encryption key
- Pairing status and timestamp

## UI States

- `nearbyDevices` - Discovered devices list
- `pairingInProgress` - Currently pairing devices
- `pairedDevices` - Successfully paired devices 