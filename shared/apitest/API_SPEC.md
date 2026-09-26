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
| 本地日历日期 | `String`，格式 `YYYY-MM-DD`，必须加 description 说明设备时区 | 用 Instant 冒充日历日（2026-09-20 起 PomodoroToday.date 已改为 Instant，由客户端按本地时区推导日历日） |
| fileId（app 文件仓内容寻址 id、相册封面/显示 URL 令牌、分片上传 fileId） | `String` | `ID`（2026-09-21 用户定：fileId 全域 String，禁止 ID 标量） |

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
- **排序**：媒体/文件列表 `sortBy: FileSortBy!` 必填；`TAKEN_AT_DESC` 只对拍摄日期分组视图有意义，其他域按各自回退序执行（docs/audio→入库时间，plain files→修改时间，packages→名称），与 FileSortBy 的 SDL description 一致（2026-09-21 核对代码后拍板：description 如实描述回退行为，不改代码）。
  例外（2026-09-24 复核拍板，刻意不带 sortBy）：`appFiles`（内容仓库浏览，固定最新在前，无排序 UI 需求）、`recentFiles`（便利查询，无分页）。

## 4. 编址体系（三个域，不混用）

| 编址 | 适用 | 例子 |
|---|---|---|
| `ID` | 实体单条操作 | `deleteTag(id)`、`updateFeed(id, …)` |
| path | 文件域（路径即身份） | `playAudio(path)`、`deleteFiles(paths)`、`renameFile(path, name)` |
| query DSL 字符串 | 批量选择（见 §5） | `trashNotes(query)`、`deleteMediaItems(type, query)` |

规则：
- 读接口返回 `id: ID!` 的实体，其**单条**写操作用 id；**批量**操作用 query；**文件域**用 path。
- id 后缀字段（`*Id` / `*Ids` / `id`）一律 `ID` 类型（例外见 §8；fileId 值域整体 String，见 §1/§8）。
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
- **空 query 守卫（2026-09-21 定）**：按 query 编址的破坏性/批量 mutation（delete/trash/restore/move/save 类，注册表由 `ApiContractTest.bulkQueryMutations` 锁死）**空串/纯空白 query 一律抛 `GraphQLError`**——空 where 会退化为 `1=1` 全表命中。全量意图必须用显式 sentinel **`all:true`** 表达（`SearchHelper.parse` 解析为 `name="all"` 字段，各域 where 构建显式忽略、不产生条件）。`deleteChatItems` 除外：空 query 走 `ChatDbHelper` 空 id 集先例（no-op）。客户端「全选」操作在无过滤条件时必须发 `all:true`，不得发空串。

## 6. 返回形状（同类操作同形状）

| 操作类别 | 返回 | 例子 |
|---|---|---|
| 批量破坏/变更（delete/trash/restore/move，按 query 或 id 列表编址，同步完成） | `ActionResult!`（`{ affectedCount: Int! }`） | `deleteSms`、`trashNotes`、`deleteMediaItems`、`deleteBookmarks`、`deleteFiles`、`deleteNotifications`、`deleteClipboardItems`、`deleteChatItems`、`deleteCalls`、`deleteContacts`、`deleteFeedEntries` |
| 单条 create/update | 实体非空 + 找不到时抛 `GraphQLError`（不返回 null） | `updateBookmark`、`createNote`/`updateNote`、`updateContact`、`updateAudioPlaylist` |
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
  - 计数字段名必须如实：`feedEntryCounts`（每 feed 条目总数，不是未读数）、`smsBoxCounts`（收件箱/已发送/草稿箱计数）、`File.childCount`（目录直接子项计数，2026-09-24 由 children 改名）。
  - 缩略图样本字段：`MediaBucket.topItemPaths`（是文件路径，不是 id）。
  - 重复 API 禁止：`fetchFeedContent` 已删（与 `syncFeedEntryContent` 实现相同，保留后者——与 UI 文案「同步正文」一致）。
  - `archiveSmsConversation(id)`：服务端自行推导会话时间（归档快照语义），客户端不传 date（2026-09-24 由 archiveConversation 改名，补齐 Sms 前缀）。
  - **2026-09-24 命名清理（breaking，多仓同周期同步）**：`File.children`→`childCount`、`ChatChannel.owner`→`ownerId`、`ChatChannelMember.id`→`peerId`（成员身份即 peer id）、`filesCount`→`fileCount`（对齐单数实体+Count）、`screenMirrorState`→`isScreenMirroring`（Boolean 不叫 State）、`syncFeedContent`→`syncFeedEntryContent`（id 是条目 id 非 feed id）、`renameAudioPlaylist`→`updateAudioPlaylist`（动词对齐 update* 且返回实体）、`archiveConversation`/`unarchiveConversation`→`archiveSmsConversation`/`unarchiveSmsConversation`、`startPomodoro(timeLeftSec)`→`startPomodoro(durationSec)`（参数是本次时长，非剩余时间；WS 事件 POMODORO_ACTION.timeLeftSec 是冻结字段不受影响）。旧名由 `ApiContractTest.legacyShapesAreGone` 锁死禁回潮。
  - **2026-09-25 命名清理（breaking，多仓同周期同步）**：`deleteClipboard`→`deleteClipboards`（批量删除按 id 列表，对齐 deleteBookmarks/deleteNotifications 复数命名）、`archivedConversations`→`archivedSmsConversations`（补齐 Sms 前缀，对齐 smsConversations 及 2026-09-24 archiveSmsConversation 改名决策）。旧名由 `ApiContractTest.legacyShapesAreGone` 锁死禁回潮。
  - **2026-09-25 第二批（breaking）**：`deleteClipboards(ids: [ID!]!)`→`deleteClipboards(query: String!)`——编址从 id 列表改为 query DSL（§4 批量操作口径；DSL 字段 ids:/text:/all，见 §5 Clipboard 行），纳入空 query 守卫（`bulkQueryMutations` 注册表）；where 构建由 `ClipboardHelper.applyClipboardFilterFields` 承担（`BulkWhereBuildersTest` 锁死）。
  - **2026-09-25 第三批（breaking）**：剪贴板族整体对齐实体单复数——`Clipboard`→`ClipboardItem`、`clipboard`→`clipboardItems`、`clipboardCount`→`clipboardItemCount`、`deleteClipboards`→`deleteClipboardItems`。旧名由 `ApiContractTest.legacyShapesAreGone` 锁死禁回潮。
  - **2026-09-26（breaking）**：删除 mutation `sendScreenMirrorControl`（连同 GraphQL 侧 `ScreenMirrorControlInput`/`TouchPointInput` input 类型）——屏幕镜像触控/控制唯一通道是 WS §12，禁止 GraphQL/WS 双方案并存（用户定）。`ScreenMirrorControlInput` 类保留，仅作 WS JSON 信封载荷（§12.5）。
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
| `ChatFiles.ids` / `ChatImages.ids` / `ChatText.linkPreviewImageIds` | `[String]` | app 文件仓 fileId（2026-09-20 用户定 String；2026-09-21 起并入全域 fileId String 政策） |
| `Audio.albumFileId` / `AppFile.id` | `String` | 相册封面显示令牌 / app 文件仓内容寻址 fileId，非实体 id（2026-09-21 用户定） |
| `uploadedChunks` / `mergeStatus` / `deleteChunks` / `mergeChunks` / `mergeAppFileChunks` 的 `fileId` 参数 | `String` | 客户端自选的分片集合 id，非实体 id（2026-09-21 用户定） |
| `deleteDbTableRows(ids: [String!]!)` | `[String]` | 调试 API，原生表主键 |
| `StorageMount.diskId` | `String` | OS 磁盘 uuid，外部标识（Android 端恒空串；2026-09-20 由 diskID 改名） |
| `PairingRequestInput.timestamp` | `Long` | 配对协议防重放字段（见 §1） |
| guest `SharedInfo.expiresAt` | `Long` | guest schema（schema-guest.graphqls）的既有 wire 契约，已被 web guest 端消费；客户端在解析边界立即转 Instant（2026-09-24 收录，见 LONG_TERM「既有 wire 例外」） |
| `chatItems(target)` / `sendChatItem(target)` | `String` | 会话目标编址串（peer id 或带前缀的 channel target，ChatTarget.parseId 解析；2026-09-20 用户定 String，非单一实体 id） |
| `DataType.DEFAULT` | 枚举成员 | 内部未赋值哨兵，客户端禁止发送（见 §7） |

## 9. 平台门控与多平台对齐

- 门控分两层：`App.capabilities: [Capability!]` 表示**设备是否提供该能力**（NAS 无 SMS 就不放 SMS；2026-09-22 用户定：`App.features`/`DeviceFeature` 更名，旧名禁用）；`App.permissions: [Permission!]` 表示**客户端访问权限**（Android 运行时权限已启用且授予）。客户端 UI 先看 capabilities 再看 permissions，**禁止嗅探 OS 版本**。
- NAS 对齐义务：同名类型的字段名/类型（ID/Instant/Long）、参数必填性、返回形状必须与主 SDL 一致；
  NAS 实现不了的语义用空值/空列表 + feature 门控，而不是改类型。
- 每媒体类型共用 `interface MediaItem {id,title,path,size,bucketId,createdAt,updatedAt}`；跨类型查询返回 `MediaItem`。

## 10. 已知待办与既定不动项

- ~~P2：`Message`/`MessageConversation` 命名、sendSms/sendMms 不对称、`ChatItemContent` union 建模、WS 事件协议文档化、`AudioPlayback` 缺 isPlaying/positionMs~~ —— 2026-09-20 第五轮已落地：SMS 域类型改名 `Sms`/`SmsConversation`/`SmsAttachment`；`ChatItemContent` 的 `ChatFiles/ChatImages.ids` → `[ID!]!`、`ChatText.ids` → `linkPreviewImageIds`（文本本体在 `content`）；`AudioPlayback` 补 `isPlaying`/`positionMs` 且 `currentPath` 空串改暴露 null；`Notification.time` → `postedAt`；`BookmarkGroup` 补 `itemCount`；sendSms/sendMms/replyNotification 已加语义 description。事件协议见 §11。
- **debug/工具 API（dbTables 系、dataStore 系、appLogs 系、sessions/events 审计系）是产品需求，常驻主 schema，不做门控/拆分（2026-09-20 用户定；2026-09-22 复核再次确认：主 schema 有意暴露这些测试/诊断 API——这不是问题，是设计，禁止再议）。**
- `DriveType` 封闭集保持现状：NAS 恒 `INTERNAL_STORAGE` + `remote: Boolean!` 区分网络挂载；将来桌面网络盘需要时**新增枚举成员是增量安全操作**。
- 编址三体系定案（2026-09-20）：批量编址 = **query DSL 字符串（§5），永久保留，禁止改成 filter input**（typed FilterInput 方案被用户否决并回滚）。path→id（音频队列）留待单独评估。

- `audioLyrics`（2026-09-22 用户定）：NAS 返回 `String!`（无歌词=空串），与手机的 nullable `String`（无歌词=null）**刻意分歧**；客户端对两端都兼容（空串与 null 同义处理）。

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
| 11 | POMODORO_ACTION | `PomodoroActionData{action:"start"|"pause"|"stop", timeLeftSec, totalTimeSec, completedCount, round, state}`（秒；2026-09-20 字段改名加 Sec 后缀，多平台同周期生效） |
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

上行（客户端→手机）控制通道见 §12。

## 12. WebSocket 上行控制协议（Screen Mirror 触控/控制通道，2026-09-26 冻结记录）

GraphQL 之外的客户端→手机实时控制旁路，协议与 plain-cast 同源（`plain-cast docs/touch-low-latency-design.md`）。屏幕镜像触控/控制**只有这一条通道**（GraphQL 无控制 mutation，2026-09-26 起）。
权威源码：`shared/src/commonMain/kotlin/com/ismartcoding/plain/httpserver/routes/WebSocketRoutes.kt`（`handleUpstreamControl`/`decodeTouchFrame`）与
`shared/src/androidMain/kotlin/com/ismartcoding/plain/services/StreamTouchInjector.kt`。改动视同 breaking。

### 12.1 连接与会话生命周期

1. `GET /`（WebSocket 升级）带 `?cid=<clientId>`；桌面访问关闭时连不上（`desktop_access_disabled`）。
2. **第一帧 = 注册帧**：ChaCha20 加密的时间戳 JSON 字符串；解密失败 `invalid_request` 关连接。
3. 注册成功后的每一帧都是**控制帧**，一律用同一 token 加密（见 12.2）。
4. 连接断开：服务端清 session 并强制释放流式触控（`resetScreenMirrorTouchStream`）——客户端断连前应先对在途手势补发 CANCEL。

### 12.2 加密

- 算法 XChaCha20-Poly1305（Tink；@noble `xchacha20poly1305` 同构），wire = `nonce(24B) || ciphertext+tag`，每帧独立加密。
- key = 32 字节 token：登录握手下发（base64），客户端 `tokenToKey`（atob）还原；服务端用 `sha512(password)` hex 前 32 字符的 ASCII 字节（`HttpServerManager.hashToToken`）。与下行 §11 同一 key。

### 12.3 控制帧分派（解密后的明文）

分派规则（热路径先查首字节、零 JSON 解析）：

| 明文首字节 | 含义 |
|---|---|
| 空 | 忽略 |
| `0x54` | 二进制触摸帧（12.4，热路径） |
| 其他 | UTF-8 JSON **类型信封**（12.5，冷路径） |

**JSON 上行一律走类型信封**：`{"type":"<注册名>", …}`（kotlinx 多态判别字段，服务端 `UpstreamMessage` sealed interface）。**裸 `ScreenMirrorControlInput` 不被接受**（防 `ignoreUnknownKeys` 误路由：不相关事件碰巧带 `action` 字段不得注入触控）。当前注册：`screenMirrorControl`（字段 `input: ScreenMirrorControlInput`）。未知 `type` / 畸形 JSON / 非法枚举：kotlinx 判别查找在读 payload 前即抛出，服务端 log 后丢弃（发送端无回执）。

**双注册表（新增上行协议的唯一入口）**：二进制 magic 字节与 JSON type 名均在此登记，禁止复用/重载已有值——新二进制协议取新 magic 字节（`0x54` 已占用），新 JSON 事件加新 type 名 + sealed 子类 + 服务端 when 分支。对齐 §11 下行事件编号的管理纪律。

| 注册名 / magic | 协议 | 载荷 |
|---|---|---|
| `0x54` | 二进制触摸帧（12.4） | 紧凑样本帧 |
| `screenMirrorControl` | 屏幕镜像控制（12.5） | `input: ScreenMirrorControlInput` |

### 12.4 二进制触摸帧（touch 热路径）

little-endian，与 plain-cast 逐字节同格式。`streamId` 本通道恒 0。

| 偏移 | 大小 | 字段 | 说明 |
|---|---|---|---|
| 0 | 1 | magic | `0x54` |
| 1 | 1 | count | 样本数（u8） |
| 2 | 2 | streamId | u16 LE，恒 0 |
| 4+8n | 8 | 样本 n | 见下 |

样本（8 字节）：`action` u8（0=DOWN 1=MOVE 2=UP 3=CANCEL，**服务端把 CANCEL 映射为 UP**）｜`pointerId` u8｜`x` u16 LE（0..65535 → 归一化 0..1）｜`y` u16 LE｜`dtMs` u16 LE（距同指上一采样毫秒数，服务端保留字段）。count=0 或长度不符整帧丢弃。

每个样本语义等价于 `TOUCH_DOWN/MOVE/UP` 的 `ScreenMirrorControlInput`；多指帧合法但 a11y 注入单流——第二指 DOWN 被忽略（plain-cast 未激活内核注入时同款行为）。

### 12.5 JSON 控制帧（`screenMirrorControl` 信封 → `ScreenMirrorControlInput`）

帧形如 `{"type":"screenMirrorControl","input":{…}}`；`input` 字段定义（归一化 Float 0..1 = 相对**渲染画面矩形**，越界值服务端钳制不丢弃）：

| 字段 | 类型 | 用途 | 必传于 |
|---|---|---|---|
| `action` | enum（下表） | 动作 | 全部 |
| `x` / `y` | Float | 画面归一化坐标 | TAP/LONG_PRESS/SWIPE/SCROLL/TOUCH_DOWN/MOVE；TOUCH_UP 可省略（沿用最后坐标） |
| `endX` / `endY` | Float | 终点坐标 | SWIPE |
| `durationMs` | Long | 时长（LONG_PRESS 缺省 500，SWIPE 缺省 300） | LONG_PRESS/SWIPE |
| `deltaX` / `deltaY` | Float | 像素增量，服务端钳 ±500 | SCROLL |
| `pathPoints` | `[TouchPointInput]` | 轨迹点 `{x: Float, y: Float, tMs: Int}`，tMs 相对首点毫秒 | TOUCH |
| `pointerId` | Int | 触控槽位（u8 域 0..255） | TOUCH_DOWN/MOVE/UP |
| `key` | String | 按键名（保留，暂只记日志） | KEY |
| `pressure` | Float | 兼容字段，服务端忽略 | — |

action 语义：

| action | 参数 | 服务端行为 |
|---|---|---|
| `TAP` | x,y | 一次 50ms 点按 |
| `LONG_PRESS` | x,y,durationMs | 一次长按（<500ms 抬到 500） |
| `SWIPE` | x,y,endX,endY,durationMs | 一次直线滑动（<50ms 抬到 50） |
| `SCROLL` | x,y,deltaX,deltaY | 合成 200ms swipe（增量钳 ±500px，0,0 忽略） |
| `BACK`/`HOME`/`RECENTS`/`LOCK_SCREEN` | 无 | 对应 `GLOBAL_ACTION_*` |
| `KEY` | key | 保留（未注入） |
| `TOUCH` | pathPoints | 整段轨迹回放：单点 tMs≥500→长按，否则点按；多点距离<4px 同上，否则全轨迹一次派发 |
| `TOUCH_DOWN`/`MOVE`/`UP` | x,y,pointerId | 流式注入（悬空长段+终结释放；边缘起始拖拽在 UP 分类：底部上滑→HOME、左右内滑→BACK、顶部下拉→通知栏） |

### 12.6 客户端选路契约

- 触摸样本（DOWN/MOVE/UP/CANCEL）**必须走二进制帧**——每秒上百样本，HTTP 逐请求不可接受；流式手势必须以 UP/CANCEL 终结，无终结帧 = 触摸滞留到 stale watchdog（10s）强制释放。
- 冷路径动作走 JSON 信封帧（12.3）。**GraphQL 无控制通道**：`sendScreenMirrorControl` mutation 已删（2026-09-26，禁双方案），WS 断开时控制帧直接丢弃——服务端断连即 `resetScreenMirrorTouchStream`（12.1），客户端无需也不得另寻通道补发。
- 无障碍服务未启用时控制帧静默丢弃（`dispatchScreenMirrorControl` 返回 false 不抛错）；开关状态以 GraphQL `screenMirrorControlEnabled` 为准。
- 同一 WebSocket 同时承载 §11 下行事件与 §12 上行控制，互不影响。
