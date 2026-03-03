# Chat & Peer Chat Test Cases

This document lists manual test cases for the App's Local Chat, Peer Chat (end-to-end encrypted), and Channel (group) features.

---

## Prerequisites

| # | Requirement |
|---|-------------|
| P-1 | At least two real devices (Device A / Device B) on the same LAN |
| P-2 | Device A and Device B are already paired (status = "paired") |
| P-3 | Device C (optional) for multi-member Channel scenarios; not directly paired with A or B |
| P-4 | Web client connected to Device A's HTTP service via browser |

---

## 1. Pairing

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| PA-1 | Device A triggers "Discover nearby devices"; Device B is in foreground | Device A discovers Device B's name and device type |
| PA-2 | Device A taps "Pair" with Device B | Device B shows a pairing request dialog |
| PA-3 | Device B taps "Allow" | Both devices enter paired status; peers table contains the other device with `status = "paired"` and a non-empty `key` |
| PA-4 | Device B taps "Reject" | Device A receives rejection feedback; dialog closes; no peer record created |
| PA-5 | Device A cancels during pairing | A sends PAIR_CANCEL; B's dialog closes; sessions cleaned up on both sides |
| PA-6 | Re-initiate pairing between already-paired devices | No duplicate peer record is created; existing pairing info is preserved |

---

## 2. Local Chat

Local chat uses `toId = "local"`; messages are stored on-device only and never sent to any peer.

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| LC-1 | Send a plain text message | Message appears in the list immediately; `status = sent` |
| LC-2 | Send a text message containing a URL | After sending, a link preview card (title / description / cover image) is fetched asynchronously and displayed |
| LC-3 | Select 1 image from the gallery and send | Placeholder thumbnail appears immediately (`content://` URI); after background import, URI is updated to `fid:` |
| LC-4 | Select 3+ images from the gallery and send | Images displayed in a grid; all URIs are imported to `fid:` |
| LC-5 | Select a non-image file and send | File name and size are shown; URI imported to `fid:` |
| LC-6 | Long-press to enter multi-select mode; select multiple messages and delete | Selected messages removed from list and database; `ref_count` decremented for each referenced `fid:` file |
| LC-7 | Delete the last message referencing a given `fid:` file | Physical file is deleted; no orphan files remain in the content-addressable store |
| LC-8 | Pull-to-refresh to load older messages | Earlier messages are appended to the top of the list |
| LC-9 | Send the same file twice | Only one FileTable record exists (same `fid:`); `ref_count = 2` |
| LC-10 | Type text in the input box, navigate away, then return | Draft text is still present (persisted via `ChatInputTextPreference`) |

---

## 3. Peer Chat (End-to-End Encrypted)

Requires two paired devices (A sends, B receives).

### 3.1 Text Messages

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| PC-1 | A sends a plain text message to B (B online) | B receives and displays the message; A's message shows `status = sent` |
| PC-2 | A sends a text with a URL to B (B online) | B receives the message and fetches the link preview asynchronously |
| PC-3 | A sends a message to B while B is offline | A's message shows `status = failed`; no automatic retry on B reconnecting (offline queue not supported) |
| PC-4 | B sends a message to A (bidirectional) | A receives the message; `fromId` = B's peer ID |

### 3.2 Image / File Transfer

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| PC-5 | A sends 1 image to B | B auto-triggers download; after completion, URI changes from `fsid:` to `fid:` |
| PC-6 | A sends 3+ images to B | B downloads each image; `download_progress` events emitted per file |
| PC-7 | A sends a large file (> 5 MB) to B | Download progress displayed correctly; after completion, URI updated to `fid:` and file opens normally |
| PC-8 | Network drops mid-download; then recovers | Download retries and completes successfully; URI changes to `fid:` |
| PC-9 | A sends the same file to B that A previously sent to local chat | A still has only 1 `fid:` file locally; `ref_count` incremented by 1 |

### 3.3 Message Actions

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| PC-10 | Long-press a message and tap "Forward" | `ForwardTargetDialog` appears; selecting a target forwards the message successfully |
| PC-11 | Enter multi-select mode and delete multiple messages in bulk | Messages deleted locally; no delete instruction sent to the peer (peer's copy unaffected) |
| PC-12 | Tap a received image to open full-screen preview | `MediaPreviewer` opens; supports pinch-to-zoom and paging |

### 3.4 Security & Encryption

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| PC-13 | Inspect HTTP traffic between A and B | Request body is ciphertext (XChaCha20-Poly1305 encrypted); no plaintext content visible |
| PC-14 | Change system clock so timestamp difference > 5 minutes, then send | B's server rejects the request (timestamp validation fails); A shows `failed` status |
| PC-15 | Simulate decryption with an incorrect shared key | Decryption fails; message is discarded; B displays nothing |

---

## 4. Channel (Group Chat)

Requires at least Device A (owner) and Device B (member); Device C is optional for non-directly-paired member scenarios.

### 4.1 Channel Creation & Management

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-1 | A creates a new Channel with a name | `chat_channels` table gets a new record; `owner = "me"`; A is auto-joined (`status = joined`) |
| CH-2 | A renames the Channel | Local name updated; `version` incremented; `ChannelUpdate` broadcast to all joined members |
| CH-3 | B receives a `ChannelUpdate` (name change) | B's local Channel name is synced; `version` updated to the new value |
| CH-4 | A deletes the Channel | A broadcasts `ChannelKick` to all members; each member's channel status set to `kicked`; channel key removed from their `channelKeyCache`; input disabled on their ChatPage |

### 4.2 Invite / Accept / Decline

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-5 | A invites B to a Channel (A and B are paired) | B receives a `channel_invite` system message and an invite notification |
| CH-5a | After inviting B, A's ChatInfoPage shows B in "Pending Members" section | Pending Members section appears with count; B's device icon is displayed |
| CH-6 | B accepts the invite | B sends `channel_invite_accept`; A promotes B from `pending` to `joined`; A broadcasts `ChannelUpdate` to all members |
| CH-6a | After B accepts, A's UI updates in real time | A's ChatInfoPage moves B from "Pending Members" to "Members"; ChatPage title count increments |
| CH-6b | After B accepts, other members (C) see updated member list | C receives `ChannelUpdate`; member list includes B as joined |
| CH-7 | B declines the invite | B sends `channel_invite_decline`; A removes B from `members` |
| CH-7a | After B declines, A's Pending Members section updates | B disappears from A's Pending Members list; count decrements |
| CH-7b | A resends invite to a pending member | A taps a pending member → taps "Resend Invite"; `channel_invite` is re-sent to B |
| CH-7c | A removes a pending member | A taps a pending member → taps "Remove"; member removed from `members` list; `ChannelUpdate` broadcast |
| CH-8 | A invites C (C is not directly paired with A or B) | On C's device, peer records for A and B are created with `status = "channel"`, `key = ""`; `publicKey` is populated |
| CH-9 | C accepts the invite and sends a message | Message is encrypted with the channel key (not a peer shared key); `c-cid` header carries the channel ID |
| CH-9a | A re-invites B after B previously left | B receives the invite; existing channel record status is restored from `left` to `joined`; channel key is re-added to B's `channelKeyCache` |
| CH-9b | A re-invites B after B was previously kicked | Same as CH-9a but from `kicked` status |

### 4.3 Member Leave & Kick

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-10 | B (non-owner) leaves the Channel voluntarily | B sends `channel_leave` to the owner; B's local channel status becomes `left`; channel key removed from B's `channelKeyCache` |
| CH-10a | After B leaves, A (owner) processes the leave | A removes B from `members`, increments `version`, broadcasts `ChannelUpdate` to all remaining members |
| CH-10b | After B leaves, remaining members (C, D) see an updated member list | C and D receive the `ChannelUpdate`; their members list no longer includes B; UI (ChatInfoPage, ChatPage title count) updates in real time |
| CH-10c | After B leaves, someone sends a message to the Channel | B does **not** receive the message (B's channel key is already removed from key cache; server rejects the request) |
| CH-10d | After B leaves, a member tries to send a message directly to B via Channel routing | The message is not forwarded to B because B is no longer in `joinedMembers()` |
| CH-11 | A (owner) kicks B from the Channel | A sends `channel_kick` to B; B's channel status set to `kicked`; B's `channelKeyCache` cleared; A removes B from the member list and broadcasts `ChannelUpdate` |
| CH-11a | After B is kicked, B tries to send a message to the Channel | Message is rejected; B's channel key has been removed from `channelKeyCache` |
| CH-11b | After B is kicked, B's ChatPage shows a notice | B sees the "You were removed from this channel" notice; input is disabled |
| CH-11c | After B leaves, B's ChatPage shows a notice | B sees the "You have left this channel" notice; input is disabled |
| CH-12 | Owner attempts to "leave" the Channel | UI shows no "Leave" option for the owner, or the action redirects to the "Delete Channel" confirmation flow |

### 4.4 Message Routing (Star Topology)

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-13 | A (leader) sends a message to the Channel | A broadcasts directly to all other joined+online members |
| CH-14 | B (non-leader) sends a message to the Channel | B sends to the leader (A); A broadcasts to all other members |
| CH-15 | Leader goes offline; B sends a message | A new leader is elected (online member with the smallest ID); message sent to the new leader |
| CH-16 | Only 1 online member remains in the Channel | `electLeader` returns that member as leader; message is self-contained; no forwarding needed |
| CH-17 | Multiple Channels share the same leader | B sends a UDP heartbeat to the leader only once per cycle (deduplicated across channels) |

### 4.5 Encryption & Security

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-18 | C (channel-only member) sends a message | Encrypted with channel key; receiver looks up the correct channel key via the `c-cid` header and decrypts successfully |
| CH-19 | A non-member device sends a `ChannelUpdate` | Receiver checks `fromId != channel.owner` and rejects the update |
| CH-20 | A `ChannelUpdate` with an older `version` arrives | Receiver ignores the update; local `version` does not roll back |

### 4.6 Permission Model

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-21 | B (member) attempts to add a new member | The "Add Member" entry is hidden or the action is rejected; only owner can add members |
| CH-22 | B attempts to rename the Channel | Rename UI is not visible to B, or the operation is rejected |
| CH-23 | B (member) sees no Pending Members section | Pending Members is only shown to the owner; B does not see it even if pending members exist |

### 4.7 UI Reactivity

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| CH-24 | A adds a member; ChatInfoPage is open | Members grid updates immediately; "Members (N)" count increments; new member appears in Pending Members |
| CH-25 | B accepts invite; A has ChatInfoPage open | A's pending member moves to joined section; "Members (N)" count increments; "Pending Members (N)" count decrements or section disappears |
| CH-26 | A removes a member; ChatPage is open | ChatPage title shows updated member count immediately |
| CH-27 | B leaves; A has ChatPage open | ChatPage title count decrements in real time |
| CH-28 | B accepts invite; A has ChatPage open (not ChatInfoPage) | ChatPage title count increments from `(N)` to `(N+1)` |
| CH-29 | A renames Channel; B has ChatPage open | B's ChatPage title updates to the new name |
| CH-30 | Channel delete by owner; B has ChatPage open | B receives kick, ChatPage shows kicked notice, input disabled |

---

## 5. Web Client Chat

The web client connects to the App's HTTP service via browser.

| # | Test Case | Expected Result |
|---|-----------|-----------------|
| WC-1 | Web sends a text message (local chat) | App receives a `message_created` WebSocket event in real time; message appears in the list |
| WC-2 | Web sends a text message to a paired peer | App forwards the message via `PeerChatHelper`; status shows `sent` |
| WC-3 | Web uploads an image and sends it | A temporary `new_xxx` message is shown immediately; after upload completes, it is replaced by the final message containing a `fid:` URI |
| WC-4 | Web uploads a large file (chunked upload) | `/upload_chunk` → `mergeChunks` → `sendChatItem`; App side receives `message_created` event |
| WC-5 | Web deletes a message | App receives `message_deleted` event; item is evicted from Apollo cache |
| WC-6 | App receives an image from a peer and is downloading it | Web receives `download_progress` events with correct progress values |
| WC-7 | App finishes downloading; URI changes to `fid:` | Web receives `message_updated` event; image renders correctly |
| WC-8 | Web accesses a `fid:` file via the `/fs` endpoint | Server resolves the `fid:` to the physical path and returns file contents |
| WC-9 | Web proxies a peer file via `/proxyfs` | Bypasses CORS / certificate issues; peer file contents returned successfully |

---

## 6. Regression Checklist

| # | Check |
|---|-------|
| R-1 | Sending the same file multiple times: `FileTable.ref_count` is correct; no duplicate physical files |
| R-2 | After deleting a message, any `fid:` file whose `ref_count` drops to 0 is physically deleted |
| R-3 | After App restart, `ChatCacheManager` (`peerKeyCache` / `channelKeyCache`) is correctly rebuilt; left/kicked channels are excluded from `channelKeyCache` |
| R-4 | Channel `version` is strictly monotonically increasing; no rollback |
| R-5 | All outgoing messages carry an Ed25519 signature; receiver verifies signature before processing |
| R-6 | Messages with a timestamp outside the 5-minute window are discarded |
| R-7 | Chat works correctly after switching networks (Wi-Fi ↔ cellular) |
| R-8 | All chat UI elements render correctly in dark mode |
| R-9 | After pairing, both devices are immediately marked as online in `onlineMap`; online status indicator updates |
| R-10 | `DChatChannel.equals()` compares all fields (not just `id`); `MutableStateFlow` emits when members / name / version change |
| R-11 | After a member leaves or is kicked, their channel key is removed from `channelKeyCache`; subsequent incoming channel messages are rejected |
| R-12 | After a member leaves, the owner broadcasts `ChannelUpdate` to all remaining members; their UI updates |
| R-13 | A left/kicked device that receives a `createChatItem` for the channel returns an empty response (message not stored) |
| R-14 | Re-invite after leave/kick restores the channel to `joined` status and re-adds the channel key to `channelKeyCache` |
