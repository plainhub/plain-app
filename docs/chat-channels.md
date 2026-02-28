# Chat Channel Design

## Overview

A `ChatChannel` is a named group conversation that can include multiple devices. Channels use a **star topology** with an elected **leader** node. All messages flow from the sender to the leader, who then broadcasts to all other members.

### Architecture

```
  B
  |
A--L--C
  |
  D
```

- **L** = Leader (elected from joined members)
- All messages: `client → leader → broadcast to other members`
- Leader is elected per-channel based on online status

---

## Key Design Decisions

### ChannelMember — Minimal (id + status only)

`ChannelMember` only stores `id` and `status` ("joined" / "pending"). All other peer metadata — name, publicKey, IP, port, deviceType — lives in the **`peers` table**.

When a peer is added to a channel but is not a directly-paired peer, a new `DPeer` record is created with `status = "channel"` and `key = ""` (no shared encryption key).

### Peer Status Values

| Status | Description |
|--------|-------------|
| `paired` | Directly paired via ECDH key exchange; has shared ChaCha20 key |
| `unpaired` | Discovered but not yet paired |
| `channel` | Known only through channel membership; no shared key; `key = ""` |

### Leader Election

A leader is elected per-channel from the **online, joined** members:

1. **Owner preferred** — if the owner is online, they are the leader.
2. **Smallest ID fallback** — otherwise, the online joined member with the smallest id.

```kotlin
fun electLeader(onlinePeerIds: Set<String>): String? {
    val myId = TempData.clientId
    val joined = joinedMembers()
    val onlineJoined = joined.filter { it.id == myId || onlinePeerIds.contains(it.id) }
    if (onlineJoined.isEmpty()) return null
    val ownerPeerId = if (owner == "me") myId else owner
    if (onlineJoined.any { it.id == ownerPeerId }) return ownerPeerId
    return onlineJoined.minByOrNull { it.id }?.id
}
```

### Encryption

| Scenario | Encryption Key | `c-cid` Header |
|----------|---------------|----------------|
| Paired peer (member has `peer.key`) | Peer's shared ChaCha20 key | Channel ID (for routing) |
| Non-paired member (`peer.key` is empty) | Channel key (`DChatChannel.key`) | Channel ID (required for decryption key lookup) |

When a member has no paired relationship with the target peer, the channel key is used for encryption, and the `c-cid` header is included so the receiver can look up the correct decryption key.

### UDP Heartbeat / IP Update

- All members periodically send their IP/port information to the leader via UDP.
- **If a member and the leader are already paired**, the existing discovery/heartbeat mechanism handles this — no extra packet needed.
- **If multiple channels have the same leader**, the member sends its IP/port update only once (deduplicated).
- The leader maintains a map of member IP addresses for message routing.

---

## Database Schema

### `ChannelMember` (serialized in `DChatChannel.members`)

```kotlin
@Serializable
data class ChannelMember(
    val id: String,
    val status: String = STATUS_JOINED,  // "joined" or "pending"
)
```

### `DChatChannel`

```kotlin
@Entity(tableName = "chat_channels")
data class DChatChannel(
    @PrimaryKey var id: String = StringHelper.shortUUID(),
) : DEntityBase() {
    var name: String = ""
    var key: String = ""                    // Base64-encoded ChaCha20 symmetric key
    var owner: String = ""                  // "me" on owner device, peer id elsewhere
    var members: List<ChannelMember> = emptyList()  // id + status only
    var version: Long = 0                   // Monotonically increasing counter
    var status: String = STATUS_JOINED      // "joined" or "left"
}
```

### `DPeer` (updated)

```kotlin
@Entity(tableName = "peers")
data class DPeer(
    @PrimaryKey var id: String,
    var name: String = "",
    var ip: String = "",
    var key: String = "",           // Empty for channel-only peers
    var publicKey: String = "",
    var status: String = "",        // "paired", "unpaired", or "channel"
    var port: Int = 0,
    var deviceType: String = "",
)
```

When a peer is created as a channel member (not directly paired):
- `status = "channel"`
- `key = ""` (no shared encryption key)
- `publicKey` is populated from the invite/accept message

---

## System Message Protocol

System messages are delivered via `PeerGraphQLClient` → `/peer_graphql`. The payload is JSON-serialized.

### Message Types

| Type | Direction | Purpose |
|------|-----------|---------|
| `channel_invite` | Owner → Invitee | Invite with channel key + member peer info |
| `channel_invite_accept` | Invitee → Owner | Accept invite + sender's publicKey/name/deviceType |
| `channel_invite_decline` | Invitee → Owner | Decline invite |
| `channel_update` | Owner → All members | Metadata changed (name, members) + member peer info |
| `channel_kick` | Owner → Kicked peer | You have been removed |
| `channel_leave` | Member → Owner | Voluntary departure |

### Payloads

```kotlin
// ChannelInvite includes MemberPeerInfo so the invitee can create
// peer records for members it doesn't already know.
data class ChannelInvite(
    val channelId: String,
    val channelName: String,
    val key: String,                    // Channel ChaCha20 key (plaintext — transport is encrypted)
    val owner: String,
    val members: List<ChannelMember>,   // id + status only
    val memberPeers: List<MemberPeerInfo>,  // name, publicKey, deviceType, ip, port for each member
    val version: Long,
)

data class MemberPeerInfo(
    val id: String,
    val name: String = "",
    val publicKey: String = "",
    val deviceType: String = "",
    val ip: String = "",
    val port: Int = 0,
)

// ChannelInviteAccept includes the accepter's info so the owner
// can create/update a peer record.
data class ChannelInviteAccept(
    val channelId: String,
    val publicKey: String = "",
    val name: String = "",
    val deviceType: String = "",
)
```

---

## Message Routing (Star Topology)

### Sending a Message

```
Sender device                           Leader device
      │                                       │
      │  if sender IS leader:                 │
      │    broadcast to all other members     │
      │                                       │
      │  if sender is NOT leader:             │
      │──── createChatItem ────────────────▶ │
      │                                       │  Leader broadcasts to all
      │                                       │  other members
```

### Implementation

```kotlin
// ChannelChatHelper.sendAsync()
suspend fun sendAsync(channel, content, onlinePeerIds): Boolean {
    val leaderId = channel.electLeader(onlinePeerIds)
    return if (leaderId == TempData.clientId) {
        broadcastAsLeader(channel, content)   // Send to all other members
    } else {
        sendToLeaderAsync(channel, leaderId, content)  // Send to leader only
    }
}
```

---

## Key Cache Architecture

`ChatCacheManager` maintains caches for fast key/public-key lookup:

| Cache | Source | Purpose |
|-------|--------|---------|
| `peerKeyCache` | `peers` table (paired peers with key) | ChaCha20 encryption/decryption |
| `peerPublicKeyCache` | `peers` table (paired + channel peers) | Ed25519 signature verification |
| `channelKeyCache` | `chat_channels` table | Channel message encryption |

Public keys for all peers (paired and channel) are now stored in the `peers` table and loaded into `peerPublicKeyCache`.

---

## Sync Flows

### Inviting a New Member

```
Owner device                            Invitee device
      │                                       │
      │  1. Add peerId to members as          │
      │     pending; version++                │
      │                                       │
      │──── ChannelInvite ──────────────────▶ │
      │     (includes memberPeers info)       │  2. Create peer records for
      │                                       │     unknown members (status="channel")
      │                                       │     Store channel locally
      │                                       │     Show invite notification
      │                                       │
      │◀─── ChannelInviteAccept ──────────── │  3a. User accepts
      │     (includes publicKey, name, type)  │
      │                                       │
      │  4a. Create/update peer record        │
      │      Move pending → joined            │
      │      Broadcast ChannelUpdate          │
      │──── ChannelUpdate ──────────────────▶ │  (all current members)
      │     (includes memberPeers for new)    │
      │                                       │
      │◀─── ChannelInviteDecline ─────────── │  3b. User declines
      │                                       │
      │  4b. Remove peerId from members       │
```

### Metadata Update (rename / manage members)

```
Owner device                            Member devices
      │                                       │
      │  1. Apply change locally              │
      │     (version++)                       │
      │                                       │
      │──── ChannelUpdate ──────────────────▶ │
      │     (includes memberPeers)            │  2. Create peer records for
      │                                       │     new members; apply update
      │                                       │     if version > local version
```

### Member Leaving the Channel

```
Leaving device                          Owner device
      │                                       │
      │──── ChannelLeave ───────────────────▶ │
      │                                       │  Remove from members; version++
      │  Delete channel locally               │  Broadcast ChannelUpdate
```

### Owner Deleting the Channel

- Broadcast `ChannelKick` to all members.
- All recipients delete the channel locally.
- Owner deletes the channel locally.

---

## UDP Heartbeat for Non-Paired Members

Members periodically update the leader with their current IP and port via UDP. Rules:

1. **Paired members skip** — if a member and the leader are already paired, the existing discovery/heartbeat mechanism provides IP/port updates.
2. **Deduplication** — if multiple channels share the same leader, the member sends the update only once per heartbeat cycle.
3. **Channel key encryption** — the UDP heartbeat packet is encrypted with the channel key for non-paired members.

---

## Permission Model

| Action | Owner | Member |
|--------|-------|--------|
| Send messages | ✅ | ✅ |
| Add members | ✅ | ❌ |
| Remove members | ✅ | ❌ |
| Rename channel | ✅ | ❌ |
| Delete channel | ✅ | ❌ |
| Leave channel | ❌ (delete instead) | ✅ |

---

## Security Model

### Transport-Layer Protection

All system messages and chat messages go through PeerGraphQL:

1. **ChaCha20Poly1305 encryption** — either peer key (paired) or channel key (non-paired).
2. **Ed25519 signature verification** — verified against `peerPublicKeyCache` (which now includes channel-only peers).
3. **Replay protection** — timestamp validation.

### Channel Key for Non-Paired Members

When a member is not directly paired with the target:
- Uses the channel key for encryption.
- The `c-cid` header identifies the channel for key lookup on the receiver.
- Signature is verified via the channel-only peer's public key in `peerPublicKeyCache`.

### Owner-Authority Verification

- `ChannelUpdate`, `ChannelKick`: rejected if `fromId != channel.owner`.
- `InviteAccept`, `InviteDecline`, `Leave`: rejected if `channel.owner != "me"`.

---

## Field Summary

| Field | Type | Location | Purpose |
|-------|------|----------|---------|
| `ChannelMember.id` | `String` | `DChatChannel.members` | Peer ID |
| `ChannelMember.status` | `String` | `DChatChannel.members` | "joined" or "pending" |
| `DPeer.status` | `String` | `peers` table | "paired", "unpaired", or "channel" |
| `DPeer.key` | `String` | `peers` table | Empty for channel-only peers |
| `DPeer.publicKey` | `String` | `peers` table | Ed25519 public key (all peer types) |
| `DChatChannel.key` | `String` | `chat_channels` table | Channel ChaCha20 symmetric key |
| `DChatChannel.owner` | `String` | `chat_channels` table | "me" on owner device; peer id elsewhere |
| `DChatChannel.version` | `Long` | `chat_channels` table | Optimistic concurrency counter |

---

## MVP Scope

1. **ChannelMember simplified** to id + status; peer metadata in `peers` table.
2. **Peer status "channel"** for non-paired channel members (`key = ""`, `publicKey` populated).
3. **Leader election** per-channel (owner preferred, online required, smallest-id fallback).
4. **Star topology** message routing: sender → leader → broadcast.
5. **Channel key encryption** for non-paired members with `c-cid` header.
6. **UDP heartbeat** to leader for IP/port updates (deduplicated across channels).
7. **MemberPeerInfo** in invite/update messages for peer record bootstrapping.

### Deferred to v2

- `managers` role (delegated admin)
- Owner transfer
- Read receipts within channels
- Channel-scoped file sharing optimisations
- Automatic leader failover with consensus
