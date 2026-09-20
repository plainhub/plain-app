# plain GraphQL API 契约规范（API_SPEC）

> 本文件是多平台 API 契约的单一规范来源。**plain-app 的 `shared/apitest/schema.graphqls` 是唯一契约源**：
> plain-nas 的 SDL 必须与其同名类型逐字段对齐；plain-desktop / 未来 iOS web 端按此 SDL 消费。
> 新增或修改任何 API 前必读本文件。结构性规则由 `shared/src/androidHostTest/.../ApiContractTest.kt` 强制锁死，
> 违反规则的 schema 改动无法通过测试。
>
> 2026-09-20 用户拍板生效。

## 0. 变更流程（改 schema 的固定动作）

1. 改 resolver/model（plain-app）。
2. 跑 `./gradlew :shared:testAndroidHostTest --tests "com.ismartcoding.plain.PrintSchemaTest"` 再生 `shared/apitest/schema.graphqls` 并提交。
3. `ApiContractTest` 必须绿（快照 + 结构约定；例外清单需同步更新本文件 §8）。
4. 同步 `apitest/groups/schema.sh` 的 expected_queries/expected_mutations 与受影响的 groups/*.sh。
5. plain-desktop 与 plain-nas 同步适配（同一次提交周期内），NAS 的 SDL 再生后必须与主 SDL 对齐。
6. 汇报中给出各仓 commit message，等用户验收提交。

## 1. 标量纪律（最重要）

| 语义 | 唯一允许的类型 | 禁止 |
|---|---|---|
| 实体标识 | `ID` | `String` |
| 时间点 | `Instant`（ISO-8601 UTC 字符串标量） | `Long` epoch、`String` |
| 字节数 / 时长（可能 >2GiB 或 >2.1e9 ms） | `Long` | `Int`（GraphQL Int 是 32 位，NAS 上 >2GiB 文件会溢出——真实 bug 教训） |
| 本地日历日期 | `String`，格式 `YYYY-MM-DD`，必须加 description 说明设备时区（先例：`PomodoroToday.date`） | 用 Instant 冒充日历日 |

时间字段的**唯一 wire 例外**：`PairingRequestInput.timestamp: Long!`（配对防重放协议的协议层字段，保持协议字节兼容）。

## 2. 单位后缀（无单位名字禁止裸奔）

- 时长：`durationMs` / `durationSec`；分钟设置项：`*Min`（`workDurationMin`）；秒计数：`*Sec`（`timeLeftSec`、`totalTimeSec`、`uptimeSec`）。
- 像素时间戳：`tMs`（触控点）。
- 禁止裸 `duration`、`timeLeft`、`totalTime`、`workDuration`、`size: Int`。ApiContractTest 按此断言。

## 3. 参数纪律（用户 2026-09-20 定，不再讨论）

- **列表参数一律必填**：`offset: Int!`、`limit: Int!`；分页列表必带 `query: String!`（空串 = 不过滤）。
  **禁止把必填参数放宽为可选/带默认值**——这不是待优化项，是既定决策，不要再提。
- **所有分页列表必须带 `query: String!` 参数**（2026-09-20 用户定）。唯一豁免：`dbTableRows`（debug DB 浏览器，debug API 按需求不动）。
- 有 offset 必有 limit，反之亦然；两者必与 filter 同现（ApiContractTest 锁死）。
- 需要展示总数的列表配 `xxxCount(filter: XxxFilter!): Int!` 兄弟字段；无总数需求的列表可以没有。
- 分页返回最新在前、页内按可直接渲染的顺序（`chatItems` 取页后 asReversed 返回旧→新）。
- **排序**：媒体/文件列表 `sortBy: FileSortBy!` 必填；`TAKEN_AT_DESC` 只对拍摄日期分组视图有意义，docs/packages 传入时按无此排序处理。

## 4. 编址体系（三个域，不混用）

| 编址 | 适用 | 例子 |
|---|---|---|
| `ID` | 实体单条操作 | `deleteTag(id)`、`updateFeed(id, …)` |
| path | 文件域（路径即身份） | `playAudio(path)`、`deleteFiles(paths)`、`renameFile(path, name)` |
| query DSL 字符串 | 批量选择（见 §5） | `trashNotes(query)`、`deleteMediaItems(type, query)` |

规则：
- 读接口返回 `id: ID!` 的实体，其**单条**写操作用 id；**批量**操作用 query；**文件域**用 path。
- id 后缀字段（`*Id` / `*Ids` / `id`）一律 `ID` 类型（例外见 §8）。
- 不允许同一功能域内混用两种编址（教训：`Bookmark.groupId: String` vs `BookmarkGroup.id: ID` 已修）。

## 5. query DSL（批量过滤器语法——冻结的客户端契约，2026-09-20 用户定：永久保留 query: String）

**`query: String!` 是既定批量编址契约，禁止改成 typed filter input**（2026-09-20 用户两次明示）。
语法（`shared-lib` `SearchHelper.parse`，行为由 `SearchHelperTest` 锁死）：

```
query      := group*
group      := "NOT" | term
term       := [field ":"] value op?
 bare word                → 全文过滤 text:word
 field:value              → 等值（op 隐含 "="）
 field:>n field:>=n field:<n field:<=n   → 数值比较（file_size:>10MB、duration:>=60）
 ids:1,2,3                → id 集合（逗号分隔）
 tag_id:<id>              → 标签（服务端解析为 ids）
 NOT field:op value       → 反选（op 反转：> ↔ <= 等）
 引号 "…" / '…' 包含空格的值；反斜杠转义
```

- 服务端把 `tag_id:` 解析成 `ids:`（`QueryHelper.parseAsync`）；多平台客户端必须逐字复刻同一语法；修改语法 = 破坏所有客户端，禁止。
- **只支持 text 过滤的分页列表**（2026-09-20 新增 query 参数的六个：notifications、appFiles、appLogs、audioQueueItems、audioPlaylistItems、audioPlayHistory；另 chatItems 在会话内 text 搜索）：query 里只有 `text:` 字段生效，其余字段忽略（`QueryHelper.textOf` 提取）。
- 各域支持的字段（冻结）：媒体=ids/tag_id/text/bucket_id/trash/artist(音频)/ext+parent+type+file_size(文档)；SMS=ids/tag_id/text/thread_id/archived/trashed；Note=ids/tag_id/text/trash；FeedEntry=ids/tag_id/text/feed_id/today/created_at；Call=ids/tag_id/text/type/duration/start_time；Contact=ids/text；Clipboard=ids/text；Package=ids/text/type；files=text。
- 新过滤器字段（新的可搜索列）是增量允许的；语法本身冻结。

## 6. 返回形状（同类操作同形状）

| 操作类别 | 返回 | 例子 |
|---|---|---|
| 批量破坏/变更（delete/trash/restore/move，按 query 或 id 列表编址，同步完成） | `ActionResult!`（`{ affectedCount: Int! }`） | `deleteSms`、`trashNotes`、`deleteMediaItems`、`deleteBookmarks`、`deleteFiles`、`deleteNotifications`、`deleteClipboard`、`deleteChatItems`、`deleteCalls`、`deleteContacts`、`deleteFeedEntries` |
| 单条 create/update | 实体非空 + 找不到时抛 `GraphQLError`（不返回 null） | `updateBookmark`、`saveNote`、`updateContact` |
| 单条 lookup query | 实体可空（null = 不存在） | `note(id)`、`feedEntry(id)` |
| 异步触发型 | `Boolean!` 或专用 pending 类型 | `uninstallPackages`、`installPackage → PackageInstallPending`、`syncFeeds` |
| 单条幂等删除 | `Boolean!` | `deleteTag`、`deleteFeed`、`deleteChatChannel` |
| 成员关系批量（打标签/入队） | `Boolean!`（保持现状，不属破坏类） | `addToTags`、`addAudiosToQueue` |

- `ActionResult.affectedCount` = 服务端确认的受影响数（deleteFiles 按实际删除计数；deleteNotifications 按请求数）。
- `MediaActionResult`（type/query 回显）已删除，不再回来。

## 7. 枚举与命名

- `MediaDataType`（AUDIO/VIDEO/IMAGE/DOC）⊂ `DataType`（同名四成员 + 其余可打标域）。
  两个枚举**不合并**；同一 GraphQL 文档跨用两者时必须声明两个变量（desktop bucketsTags 先例，SDL description 已写明）。
- `DataType.DEFAULT` 是内部哨兵成员，保留在 wire 枚举里（删除会级联 nullable 改造，2026-09-20 复核维持）；
  **客户端禁止发送它**；新枚举禁止再携带内部哨兵成员。
- 命名规则：
  - 计数字段名必须如实：`feedEntryCounts`（每 feed 条目总数，不是未读数）、`smsBoxCounts`（收件箱/已发送/草稿箱计数）。
  - 缩略图样本字段：`MediaBucket.topItemPaths`（是文件路径，不是 id）。
  - 重复 API 禁止：`fetchFeedContent` 已删（与 `syncFeedContent` 实现相同，保留后者——与 UI 文案「同步正文」一致）。
  - `archiveConversation(id)`：服务端自行推导会话时间（归档快照语义），客户端不传 date。
- 配对/发现域的 `platform: String` 是自由字符串（`android`/`ios`/`macos`…，QR 配对可为空串）；
  **类型化枚举只有 `DeviceInfo.platform: DevicePlatform`**。发现协议不保证枚举闭包，勿改。

## 8. 刻意例外清单（与 ApiContractTest.stringIdAllowlist 一一对应，改这里必须同步改测试）

| 字段/参数 | 类型 | 理由 |
|---|---|---|
| `Contact.photoId` / `Call.photoId` / `Contact.thumbnailId` | `String` | Android RawContact 照片行 id，Android-only 功能，非本 API 实体 |
| `App.clientId` | `String` | 客户端公开身份串，非可编址实体 |
| `subscriptionId`（Sim 字段、sendSms 参数） | `Int` | Android SIM 订阅整数（槽位序号） |
| `FeedEntry.rawId` | `String` | 上游 RSS guid，外部标识 |
| `sendMms/sendSms requestId` | `String` | 幂等键，非实体 id |
| `ScreenMirrorControlInput.pointerId` | `Int` | 多指触控槽位序号 |
| `ChatFiles.ids` / `ChatImages.ids` / `ChatText.linkPreviewImageIds` | `[String]` | app 文件仓 fileId（2026-09-20 用户定：String，禁止 ID 标量） |
| `deleteDbTableRows(ids: [String!]!)` | `[String]` | 调试 API，原生表主键 |
| `PomodoroToday.date` | `String` | 本地日历日（见 §1） |
| `PairingRequestInput.timestamp` | `Long` | 配对协议防重放字段（见 §1） |
| `DataType.DEFAULT` | 枚举成员 | 内部未赋值哨兵，客户端禁止发送（见 §7） |

## 9. 平台门控与多平台对齐

- 能力门控走 `App.features: [DeviceFeature!]`，**禁止客户端嗅探 OS 版本**。
- NAS 对齐义务：同名类型的字段名/类型（ID/Instant/Long）、参数必填性、返回形状必须与主 SDL 一致；
  NAS 实现不了的语义用空值/空列表 + feature 门控，而不是改类型。
- 每媒体类型共用 `interface MediaItem {id,title,path,size,bucketId,createdAt,updatedAt}`；跨类型查询返回 `MediaItem`。

## 10. 已知待办与既定不动项

- ~~P2：`Message`/`MessageConversation` 命名、sendSms/sendMms 不对称、`ChatItemContent` union 建模、WS 事件协议文档化、`AudioPlayback` 缺 isPlaying/positionMs~~ —— 2026-09-20 第五轮已落地：SMS 域类型改名 `Sms`/`SmsConversation`/`SmsAttachment`；`ChatItemContent` 的 `ChatFiles/ChatImages.ids` → `[ID!]!`、`ChatText.ids` → `linkPreviewImageIds`（文本本体在 `content`）；`AudioPlayback` 补 `isPlaying`/`positionMs` 且 `currentPath` 空串改暴露 null；`Notification.time` → `postedAt`；`BookmarkGroup` 补 `itemCount`；sendSms/sendMms/replyNotification 已加语义 description。事件协议见 §11。
- **debug/工具 API（dbTables 系、dataStore 系）是产品需求，常驻主 schema，不做门控/拆分（2026-09-20 用户定，禁止再议）。**
- `DriveType` 封闭集保持现状：NAS 恒 `INTERNAL_STORAGE` + `remote: Boolean!` 区分网络挂载；将来桌面网络盘需要时**新增枚举成员是增量安全操作**。
- 编址三体系定案（2026-09-20）：批量编址 = **query DSL 字符串（§5），永久保留，禁止改成 filter input**（typed FilterInput 方案被用户否决并回滚）。path→id（音频队列）留待单独评估。

## 11. WebSocket 事件协议（旁路实时契约，2026-09-20 冻结记录）

GraphQL schema 没有 Subscription；实时变更走专用 WS 旁路。**事件名/编号/payload 一旦多平台消费即冻结**，改动视同 breaking（新增事件/字段为增量安全；重命名/复用编号禁止）。权威源码：`shared/src/commonMain/kotlin/com/ismartcoding/plain/events/WebSocketEvents.kt`（EventType 枚举 + payload data class）与各 `sendEvent(WebSocketEvent(...))` 调用点。

**帧格式**（SJCL 加密通道解密后）：`[4 字节大端 int32 事件编号][payload 字节]`。payload 除下列 raw-binary 事件外均为 UTF-8 JSON。

**Raw-binary 事件**（payload 不是 JSON）：`SCREEN_MIRROR_VIDEO(31)`、`SCREEN_MIRROR_AUDIO(33)`、`IMAGE_EDITOR_UPDATE(34)`（编辑器增量帧）。

**事件注册表**（编号冻结；payload 类型标注 Kotlin data class 或字面形状）：

| 编号 | 事件 | payload |
|---|---|---|
| 1 | MESSAGE_CREATED | JSON `[ChatItem…]` |
| 2 | MESSAGE_DELETED | JSON 字符串，两种形态：query DSL `"ids=a,b,c"` 或单 id（消费端两种都要兼容） |
| 3 | MESSAGE_UPDATED | JSON `[ChatItem…]` |
| 4 | FEEDS_FETCHED | `{feedId, error}`（feedId="all" 表示全量同步） |
| 5 | SCREEN_MIRRORING | `{"running": bool}` |
| 7/8/9 | NOTIFICATION_CREATED / UPDATED / DELETED | model 列表或 id 列表（见 PNotificationListenerService） |
| 10 | NOTIFICATION_REFRESHED | 空（触发客户端重新拉取 `notifications`） |
| 11 | POMODORO_ACTION | `PomodoroActionData{action:"start"|"pause"|"stop", timeLeft, totalTime, completedCount, round, state}`（秒） |
| 12 | POMODORO_SETTINGS_UPDATE | DPomodoroSettings JSON |
| 14 | SCREEN_MIRROR_AUDIO_GRANTED | JSON bool |
| 15 | BOOKMARK_UPDATED | 空（重新拉取 `bookmarks`/`bookmarkGroups`） |
| 16 | DOWNLOAD_PROGRESS | `[DownloadProgressItem{id, messageId, downloadedSize, size, downloadSpeed, status}]` |
| 17 | MMS_SENT | 见 MMS_SEND_RESULT 流程 |
| 18 | CHANNELS_UPDATED | 空（重新拉取 `chatChannels`） |
| 19 | IMAGE_SEARCH_UPDATED | ImageSearchStatus JSON（同 GraphQL `imageSearchStatus`） |
| 20 | PEER_STATUS_UPDATED | `PeerStatusData{id, online}` |
| 21 | DEVICE_NAME_UPDATED | JSON 字符串（新设备名） |
| 22 | PAIRING_REQUEST_RECEIVED | DPairingRequest JSON |
| 23/24/25/26 | PAIRING_SUCCESS / FAILED / CANCELED / STARTED | `DPairingResult{deviceId, deviceName, error?}` |
| 27 | NEARBY_DEVICE_FOUND | DNearbyDevice JSON（含 status） |
| 29/30 | NEARBY_DISCOVERY_STARTED / STOPPED | `"{}"` |
| 31/32/33 | SCREEN_MIRROR_VIDEO / VIDEO_CODEC / AUDIO | 31/33 为裸媒体流；32 为 `ScreenMirrorVideoCodec` JSON |
| 34 | IMAGE_EDITOR_UPDATE | 编辑器增量二进制帧 |
| 35 | SMS_PROVIDER_CHANGED | `{uris: [...]}` |
| 36 | SMS_SEND_RESULT | `SmsSendResultData{requestId?, success, resultCode}`（-1000=超时，-1001=取消） |
| 37 | MMS_SEND_RESULT | `MmsSendResultData{pendingId, success, resultCode}`（sendMms 返回的 pendingId 在此回结） |
| 38 | UPLOAD_MERGE_RESULT | `UploadMergeResultData{fileId, ok, value?, mergedSize?, error?}` |
| 39 | CLIPBOARD_CHANGED | `ClipboardChangedData{text, sensitive, time}`（time=epoch millis 例外，见 docs/clipboard-sync.md） |
| 40 | PERMISSIONS_UPDATED | JSON 字符串数组（已授权权限名快照，同 `app.permissions`） |

编号 6/13/28 已跳过不存在，**禁止回收复用**。新增事件顺次取下一个空闲编号。
