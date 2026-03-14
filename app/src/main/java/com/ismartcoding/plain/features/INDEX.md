# features/ — Domain Logic by Feature

> 60+ files | Package: `com.ismartcoding.plain.features`

## Top-Level (7 files)

| File | Purpose |
|------|---------|
| AudioPlayer.kt | Media3 MediaController: isPlayingFlow, play/pause/seek, playlist |
| BookmarkHelper.kt | Bookmark CRUD, groups, JSON import/export, favicon download |
| LinkPreviewHelper.kt | OG meta extraction (title, description, image), favicon, 10MB limit |
| NoteHelper.kt | Note CRUD: count, search, soft delete, ContentWhere query parsing |
| PackageHelper.kt | App install/uninstall, cached labels, icon→PNG, X509 cert extraction |
| Permissions.kt | Permission enum with `can()`, `grant()`, runtime + special intents |
| TagHelper.kt | Tag CRUD: count by DataType, getAll, add/delete relations |

## bluetooth/ (9 files)
BLE scanning, connecting, read/write operations.
- `BluetoothUtil.kt` — Core BLE adapter, scan settings, UUID filter
- `Bluetooth.kt` — Scan callbacks, device discovery
- `BTDevice.kt` / `SmartBTDevice.kt` — Device data wrappers
- `BluetoothPermission.kt` — Permission checks
- `BluetoothEvents.kt` / `BluetoothActionType.kt` / `BluetoothActionResult.kt` / `IBTOperation.kt`

## book/ (1 file)
- `BookHelper.kt` — Book CRUD, EPUB import/export

## call/ (2 files)
- `BlockedNumberHelper.kt` — Blocked number list management
- `SimHelper.kt` — Multi-SIM info

## contact/ (3 files)
- `ContentHelper.kt` — ContactsProvider fields (events, emails, phones, addresses, IMs, orgs)
- `GroupHelper.kt` — Contact groups/labels
- `SourceHelper.kt` — Contact source accounts

## feed/ (6 files)
- `FeedHelper.kt` — Feed CRUD, OPML import/export, fetch scheduling
- `FeedEntryHelper.kt` — Feed entry search, count
- `HtmlUtils.kt` — HTML cleanup for feed content
- `Extensions.kt` — Feed-specific extensions
- `FeedAutoRefreshInterval.kt` — Refresh interval enum
- `FeedWorkerStatus.kt` — Worker status tracking

## file/ (4 files)
- `FileSystemHelper.kt` — File system traversal, storage mounts, filtering
- `DFile.kt` — File data class
- `DStorageStatsItem.kt` — Storage stats
- `FileSortBy.kt` — Sort options enum

## locale/ (1 file)
- `LocaleHelper.kt` — `getString(resId)`, `getStringIdentifier()`, `currentLocale()`

## media/ (9 files)
MediaStore content-resolver based helpers.
- `BaseMediaContentHelper.kt` — Abstract base for MediaStore queries (cursor, search, paging)
- `BaseContentHelper.kt` — Extends base, adds filtering/sorting
- `AudioMediaStoreHelper.kt` — Audio from MediaStore
- `ImageMediaStoreHelper.kt` — Image from MediaStore
- `VideoMediaStoreHelper.kt` — Video from MediaStore
- `FileMediaStoreHelper.kt` — Files by bucket from MediaStore
- `ContactMediaStoreHelper.kt` — Contact photos
- `CallMediaStoreHelper.kt` — Call logs
- `CastPlayer.kt` — Chromecast/UPnP DLNA casting

## sms/ (4 files)
- `SmsHelper.kt` — SMS/MMS threads, messages, conversations
- `SmsConversationHelper.kt` — Conversation queries and filtering
- `MmsHelper.kt` — MMS-specific parsing (addresses, parts)
- `DMessage.kt` — Message entity data classes
