# web/ — HTTP Server & GraphQL API

> 50+ files | Package: `com.ismartcoding.plain.web`

## Server Core (10 files)

| File | Purpose |
|------|---------|
| HttpServerManager.kt | Ktor server lifecycle: start/stop, SSL/TLS, port alloc, auth token cache, WS sessions |
| HttpModule.kt | Ktor module: routes, middleware, error handling |
| MainGraphQL.kt | Primary GraphQL schema: 100+ queries/mutations (audio, video, image, contact, SMS, feeds, notes, files, chats, peers, packages...) |
| PeerGraphQL.kt | Peer-to-peer GraphQL schema (inter-device communication subset) |
| AuthInput.kt | Password-based authentication input |
| GraphQLError.kt | Custom GraphQL error type |
| SessionList.kt | Web session tracking/management |
| AbortableRequestHandler.kt | Cancellable request handler |
| NsdHelper.kt | NSD (Bonjour) service discovery: register/unregister |
| MdnsNsdReregistrar.kt | Re-register mDNS on network change |

## loaders/ — GraphQL Data Loaders (4 files)

| File | Purpose |
|------|---------|
| FeedsLoader.kt | Batch-load feeds with entry counts |
| FileInfoLoader.kt | Batch-load file metadata |
| MountsLoader.kt | Batch-load storage mounts |
| TagsLoader.kt | Batch-load tags with counts |

## models/ — GraphQL Response Types (41 files)

Response models that mirror data/ entities for API serialization.

**Core**: App, Audio, Battery, Bookmark, Call, ChatChannel, ChatItem, Contact, ContactGroup, ContactSource, DeviceInfo, Feed, FeedCount, FeedEntry, File, FileInfo, Image, MediaBucket, Message, MessageConversation, Note, Notification, Package, Peer, Sim, StorageMount, Tag, TagRelation, Video

**Input types**: ContactInput, NoteInput, ActionResult, ID

**Specialized**: DPhoneNumber, FavoriteFolder, PackageInstallPending, PhoneGeo, PomodoroSettings, PomodoroToday, ScreenMirrorQuality, TempValue

## websocket/ — WebSocket Handling (3 files)

| File | Purpose |
|------|---------|
| WebSocketHelper.kt | Broadcast WS events (XChaCha20 encrypted or binary), WebRTC signaling |
| WebSocketSession.kt | WS session wrapper (clientId, session ref) |
| WebRtcSignalingMessage.kt | WebRTC SDP/ICE signaling message types |
