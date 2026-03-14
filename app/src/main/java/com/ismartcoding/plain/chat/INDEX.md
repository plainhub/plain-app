# chat/ — Peer-to-Peer & Group Chat System

> 15+ files | Package: `com.ismartcoding.plain.chat`

## Core (8 files)

| File | Purpose |
|------|---------|
| ChatDbHelper.kt | Chat persistence: sendAsync() saves DMessageContent, from/to/channel IDs, status |
| ChatCacheManager.kt | In-memory message cache with TTL expiry |
| PeerChatHelper.kt | 1:1 peer message routing and sending |
| ChannelChatHelper.kt | Group channel messaging |
| PeerGraphQLClient.kt | GraphQL client for peer-to-peer queries |
| ChannelSystemMessage.kt | System message types (join, leave, member changes) |
| ChannelSystemMessageHandler.kt | Process incoming system messages |
| ChannelSystemMessageSender.kt | Send system messages to channels |

## discover/ — Peer Discovery (3 files)

| File | Purpose |
|------|---------|
| NearbyDiscoverManager.kt | Google Nearby Connections discovery |
| NearbyNetwork.kt | Local network peer discovery (mDNS/Bonjour) |
| NearbyPairManager.kt | Pairing workflow: ECDH key exchange, state machine |

## download/ — File Transfers (5 files)

| File | Purpose |
|------|---------|
| DownloadQueue.kt | Queue for peer file downloads |
| DownloadTask.kt | Individual download task state |
| DownloadStatus.kt | Status enum (pending/downloading/completed/failed) |
| DownloadProgress.kt | Progress tracking data class |
| PeerFileDownloader.kt | HTTP-based peer file downloader |
