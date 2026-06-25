# PeerCacher 改造 Plan

## 目标
把 `PeerViewModel` 里 5 个本地 state 搬到共享的 `PeerCacher`（用 `StateFlow`），让
`PeerManager` 持有 Cacher 并管理写入，ViewModel 和 GraphQLAPI 直接读写 Cacher / 调
Manager，避免到处发 event 同步状态。

## 当前状况

`PeerViewModel` 当前持有 5 个本地 mutable state，作为 UI 唯一真相源：
- `pairedPeers: SnapshotStateList<DPeer>`
- `unpairedPeers: SnapshotStateList<DPeer>`
- `latestChatMap: SnapshotStateMap<String, DChat>` (private)
- `onlineMap: SnapshotStateMap<String, Boolean>`
- `onlinePeerIds: SnapshotStateSet<String>`

写操作散落在多处：
- `PeerManager.deletePeer/markUnpaired/applyDeviceDiscovered/upsertPaired` 只改 DB，需要 ViewModel 监听 `PeerUnpairedEvent`/`HMessageCreatedEvent`/`NearbyDeviceFoundEvent` 调 `loadPeers()` 重读
- `PeerStatusManager.setOnline` 发 `PeerOnlineStatusChangedEvent` 触发 ViewModel 重新 sort
- `ChatMessageGraphQL` 发 `HMessageCreatedEvent` 让 ViewModel 重新查 latestChatMap

读取散落在 9 个 UI 文件。

## 改造后

### 写入路径（唯一入口：PeerManager）
```
PairingPeerStore.upsertPaired() ───┐
NearbyViewModel.unpairDevice() ────┼─► PeerManager ─► 改 DB ─► 同步 PeerCacher
ChatPeerGraphQL (deletePeer/unpair)┤
PeerChatSender (after DB update) ──┤
PeerStatusManager.setOnline() ─────┘
```

### 读取路径（共享 StateFlow）
```
PeerCacher.pairedPeers ─► collectAsStateValue() ─► HomeChatWidget, ChatListPage, ...
PeerCacher.onlinePeerIds ─► collectAsStateValue() ─► ChatPage.sendMessage
PeerCacher.latestChatMap ─► getLatestChat() ─► HomeChatWidget
```

### 改造点

| 文件 | 改造 |
| --- | --- |
| `chat/peer/PeerCacher.kt` | 5 个 `MutableStateFlow` + 更新方法 |
| `chat/peer/PeerManager.kt` | 写方法末尾同步 `PeerCacher`；新增 `refreshFromDb()`、`setOnlineStatus()` |
| `chat/peer/PeerStatusManager.kt` | `setOnline` 调 `PeerManager.setOnlineStatus`，删 `PeerOnlineStatusChangedEvent` |
| `ui/models/PeerViewModel.kt` | 删除 5 个本地 state；删除 init event listener；只保留 `updateDiscoverable` |
| `chat/ChatSender.kt` / `ChatMessageReceiver.kt` | 写 chat 后调 `PeerManager.refreshLatestChats()` |
| `ui/models/NearbyViewModel.kt` | `unpairDevice` 不再发 `PeerUnpairedEvent` |
| `ui/page/home/chat/HomeChatWidget.kt` | `peerVM.pairedPeers` → `PeerCacher.pairedPeers.collectAsStateValue()` |
| `ui/page/chat/ChatListPage.kt` | 同上 + unpairedPeers |
| `ui/page/chat/NearbyPage.kt` | `peerVM.pairedPeers` → `PeerCacher` |
| `ui/page/chat/ChatPage.kt` / `ChatPageFileHandler.kt` / `ForwardTargetDialog.kt` | `peerVM.onlinePeerIds` → `PeerCacher` |
| `ui/page/chat/ChannelInfoPage.kt` | `peerVM.pairedPeers` → `PeerCacher` |
| `ui/page/home/HomePage.kt` / `MainEventHandler.kt` | 删除 `peerVM.loadPeers()`，改用 PeerManager |
| `ui/MainActivity.kt` | `peerVM.onlinePeerIds` → `PeerCacher` |
| `ui/MainActivityIntentHandler.kt` | `peerVM.loadPeers()` → `PeerManager.refreshFromDb()` |
| `ui/page/chat/ChatPageEffects.kt` | 删除 `peerVM.loadPeers()` |
| `events/AppEvents.kt` | 删除 `PeerUnpairedEvent` / `PeerOnlineStatusChangedEvent` |

## 边界

- Chat 列表本身的 UI state (`ChatViewModel`) 不动，HMessageCreatedEvent 在
  ChatPage/ChatViewModel 里仍然需要（驱动消息滚动等）。
- `ChatCacheManager` (peerKeyCache 等) 不动 — 那是底层 cache，不直接驱动 UI。
- 初始化时机：Application/ViewModel 启动时调 `PeerManager.refreshFromDb()` 一次性
  从 DB 加载 peer 列表 + latestChats + onlineMap（基于当前 socket 状态）。

## 风险

- `latestChatMap` 在多处写入（ChatSender + ChatMessageReceiver），需要确保都触发
  refresh，否则 UI 排序会过期。改造时统一加 `PeerManager.refreshLatestChats()` 调
  用。
- `setOnline` 现在是高频调用（每次 socket 状态翻转），确认 `PeerCacher.setOnline`
  是幂等且 cheap（同一 peerId 同一 online 直接 return）。
- 启动顺序：app 启动后 `MainActivity` 创建 PeerViewModel 时调
  `PeerManager.refreshFromDb()`，在 `PeerStatusManager.start()` 之前完成。