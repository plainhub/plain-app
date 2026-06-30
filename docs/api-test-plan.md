# API Test Plan — PlainApp GraphQL Surface

This plan exercises every `query` / `mutation` declared under
`app/src/main/java/com/ismartcoding/plain/web/schemas/`. Tests are organised
into 12 groups, ordered by **dependency on real device state**: we start
from read-only device info that needs no fixtures, then work through
content-provider reads, app-owned CRUD, file system, networking, and chat.

For each case the harness does two things:

1. **API call** — `POST /graphql` with `c-id` + `Authorization: Bearer <token>`.
2. **Ground truth** — a command run through `adb -s $ADB_ID shell ...` that
   reads the same value from Android (`getprop`, `pm list`, `content query`,
   direct DB pull, etc.).

A case passes only if both halves agree.

## How to run

```bash
# 1. fill in apitest/config.json (adb_id, url, cid, token)
# 2. run a single group
./apitest/runner.sh setup
# 3. or run several groups by name
./apitest/runner.sh setup device-read content-provider
# 4. or run everything
./apitest/runner.sh
```

Group names map directly to files in `apitest/groups/<name>.sh`.
Raw API responses land in `apitest/results/<group>-*.json` (gitignored).
Findings worth preserving are written back into the group script as
inline comments so the next run starts with the right assumptions
baked in.

## Test groups

| Group              | Schemas covered                              | Cases | Ground truth source |
|--------------------|----------------------------------------------|------:|---------------------|
| `setup`            | `AppGraphQL` + introspection                 |    14 | `getprop` / introspection |
| `device-read`      | `PackageGraphQL`, `DataStoreGraphQL`, `DbGraphQL` |    18 | `pm list`, preference DB, `plain.db` |
| `content-provider` | `ContactGraphQL`, `SmsGraphQL`, `CallGraphQL` |    24 | `content query --uri content://...` |
| `notes`            | `NoteGraphQL`, `TagGraphQL`, `FeedGraphQL`   |    30 | `plain.db` notes + tags + feed_entries |
| `media`            | `AudioGraphQL`, `VideoGraphQL`, `ImageGraphQL`, `MediaGraphQL`, `DocGraphQL` |    25 | MediaStore via `content query` |
| `files`            | `FileQueryGraphQL`, `FileMutationGraphQL`, `FileUploadGraphQL` |    17 | filesystem walk + DB |
| `app-state`        | `BookmarkGraphQL`, `PomodoroGraphQL`, `NotificationGraphQL`, `AppLogsGraphQL`, `AppFileGraphQL` |    22 | DB rows + dumpsys |
| `screen-mirror`    | `ScreenMirrorGraphQL`                        |     9 | adb input events (offer/answer/ICE) |
| `discovery`        | `DiscoverGraphQL`, `PairingGraphQL`, `ChatPeerGraphQL` |     9 | `nds` / `peers` table |
| `chat-channels`    | `ChatChannelGraphQL`                         |     9 | channels table |
| `chat-messages`    | `ChatMessageGraphQL`                         |     6 | chat_items table |
| `schema`           | static `__schema` introspection              |     4 | static introspection |

Total: 12 groups covering all 177 operations declared under
`app/src/main/java/com/ismartcoding/plain/web/schemas/`.

## Group-level plan

### `setup` — setup + sanity (AppGraphQL + introspection)

| Case | API | Ground truth | Notes |
|------|-----|--------------|-------|
| setup-C01  | `app { appVersion }`                     | `getprop persist.sys.app_version` or `dumpsys package com.ismartcoding.plain \| grep versionName` | version string must match `BuildConfig.VERSION_NAME` |
| setup-C02  | `app { clientId }`                       | extract from running session table              | must equal `config.cid` |
| setup-C03  | `app { httpPort }`                       | `adb shell netstat -tln \| grep 8080`          | port must be 8080 |
| setup-C04  | `app { httpsPort }`                      | `adb shell netstat -tln \| grep 8443`          | port must be 8443 |
| setup-C05  | `app { deviceName }`                     | `adb shell settings get global device_name`    | |
| setup-C06  | `app { appDir }`                         | `adb shell pm path com.ismartcoding.plain`     | should end with `/data/data/com.ismartcoding.plain` |
| setup-C07  | `app { internalStoragePath }`            | `adb shell echo $EXTERNAL_STORAGE`             | |
| setup-C08  | `app { sdcardPath }`                     | `adb shell sm list-volumes`                    | null if no SD |
| setup-C09  | `app { debug }`                          | `getprop ro.debuggable`                        | both true on userdebug |
| setup-C10  | `app { versionCode }`                    | `dumpsys package com.ismartcoding.plain \| grep versionCode` | integer |
| setup-C11  | `deviceInfo { osVersion }`               | `getprop ro.build.version.release`             | |
| setup-C12  | `battery { level }`                      | `dumpsys battery \| grep level`                | 0..100, must match within 2% |
| setup-C13  | introspection `__schema { queryType { name } }` | static — schema name should be `Query` | |
| setup-C14  | introspection `__schema { mutationType { name } }` | static — schema name should be `Mutation` | |

### `device-read` — device-wide read

`PackageGraphQL`, `DataStoreGraphQL`, `DbGraphQL`.

Cases cover: `packages` count, `packages` first page, `packages` search
partial match (id / name / cert issuer / cert subject), `dataStore` key
list, `dataStore` value for known key, `dataStore` for unknown key,
`db` size in MB, `db` `note` count, `db` `tag` count,
`db` `chat_message` count, `db` `peer` count, etc.

Ground truth: `pm list packages -3 | wc -l` for installed apps, pull
`/data/data/com.ismartcoding.plain/databases/plain.db` via `run-as`,
count rows in each table.

### `content-provider` — Contact + Sms + Call

`ContactGraphQL`, `SmsGraphQL`, `CallGraphQL`.

Cases:
- `contacts(offset, limit)` count vs `content query --uri content://contacts/people/ --projection _id:... | wc -l`
- `contacts` first row's `name` matches `content query` first row
- `smses` count vs `content query --uri content://sms/`
- `smses` first row's `address` + `body` matches
- `calls` count vs `content query --uri content://call_log/calls/`
- `calls` first row's `number` + `type` matches
- edge cases: empty search query, large offset (out of range)

### `notes` — notes / tags / feed CRUD

`NoteGraphQL`, `TagGraphQL`, `FeedGraphQL`.

Lifecycle per entity: create → read → update → list → delete → verify gone.
For each, ground truth is the corresponding table in `plain.db`.

- Note: `noteCount` → saveNote → noteCount+1 → query by id → trashNotes → restoreNotes → deleteNotes
- Tag: `tags` → saveTag → tags contains it → deleteTag
- Feed: `feedEntries` → saveFeedEntry → check entry count → trash → delete
- Cross: `saveFeedEntriesToNotes` moves feed entries into notes table

### `media` — audio / video / image / media / doc reads

`AudioGraphQL`, `VideoGraphQL`, `ImageGraphQL`, `MediaGraphQL`, `DocGraphQL`.

For each: count vs MediaStore count (`content query --uri content://media/external/audio/media/`), first row's `name`/`size`/`duration` cross-check.

Also: `audio` `playUrl` is a valid URL (`/stream/...`), `image` `thumbnailPath` exists on device (`adb shell ls`).

### `files` — file query / mutation / upload

`FileQueryGraphQL`, `FileMutationGraphQL`, `FileUploadGraphQL`.

Cases:
- `files(path: "/")` returns directory listing, cross-checked with `adb shell ls /sdcard`
- `files(path: "/Download")` count vs adb
- `fileUsage` returns a non-empty map
- `deleteFiles` on a temp file created via adb push
- `renameFile` round-trip
- `uploadFile` via multipart — upload a small file, verify it's listed under the target dir

### `app-state` — bookmarks / pomodoro / notification / applogs / appfile

`BookmarkGraphQL`, `PomodoroGraphQL`, `NotificationGraphQL`, `AppLogsGraphQL`, `AppFileGraphQL`.

- `bookmarks` vs bookmarks table
- `pomodoro` current state, `setPomodoro` → state changes
- `notifications` count vs `dumpsys notification | grep com.ismartcoding.plain | wc -l`
- `appLogs` returns last N log lines
- `appFiles` returns internal storage listing

### `screen-mirror` — MediaCodec + WebCodecs pipeline

`ScreenMirrorGraphQL`. Phone captures via `MediaProjection` and encodes
H.264 (video) + Opus (audio) with `MediaCodec`; web decodes with
`WebCodecs` (`VideoDecoder` / `AudioDecoder`).

State machine: `screenMirrorVideoCodec` (proactive avcC config pull) →
`startScreenMirror` (begins capture) → binary ws frames
`screen_mirror_video` / `screen_mirror_audio` → `stopScreenMirror`.
Verify state via `dumpsys media_session` and `screenMirrorState` query.

### `discovery` — mDNS discover / pairing / peer

`DiscoverGraphQL`, `PairingGraphQL`, `ChatPeerGraphQL`.

- `discover` triggers mDNS browse, returns nearby devices
- `discover` returns our own device info (verify against `getprop`)
- `peers` lists paired devices (likely empty on a single-device test rig)
- `pairingStatus` reflects current state
- `createPairing` is a no-op when no nearby device is accepting

### `chat-channels` — channel CRUD + membership

`ChatChannelGraphQL`.

- `chatChannels` empty list
- `createChatChannel` with one member → `chatChannels` has one entry
- `updateChatChannel` rename
- `inviteToChatChannel` add a second member
- `kickFromChatChannel` remove that member
- `leaveChatChannel` → channel disappears for the leaver
- `deleteChatChannel` → channel removed for all

### `chat-messages` — local / peer / channel messages

`ChatMessageGraphQL`.

Three targets: `toId: "local"`, `toId: "peer:..."`, `toId: "channel:..."`.

- `chatMessages` count vs `chat_items` table count
- `sendChat` to local → message count + 1
- `sendChat` to channel → message appears in both sender and recipient views
- `trashChatItems` / `restoreChatItems` / `deleteChatItems` lifecycle
- `markChatItemAsRead` flips `unread_count` to 0

### `schema` — `__schema` introspection sanity

`SchemaTypesGraphQL` + introspection baked into the runner.

- `__schema { types { name } }` returns the full declared type list
- every `query` in MainGraphQL is reachable via introspection
- every `mutation` is reachable
- types listed in our `models/` package are all in the schema

---

## After each group

1. The runner prints `pass / fail` to stdout.
2. Raw API responses saved under `apitest/results/<group>-*.json`
   (gitignored — they may contain device-side data).
3. Findings worth preserving (schema corrections, real bugs, behavioural
   surprises) are written into the group script as inline comments so
   the next run starts with the right assumptions baked in.

## Scope discipline

- No data is destroyed on the device without first creating it via the
  test itself (or via adb as part of setup). When the test needs a real
  contact / SMS / call to exist, we insert a fixture via `content insert`
  on adb, verify the API sees it, and then delete the fixture.
- Bearer token never leaves the harness machine; results files contain
  the API response but the `Authorization` header is stripped from any
  dump we archive.
- Bearer token rotation: if `config.json` is rotated mid-run, the next
  case fails fast with `401`; the runner exits and the operator can rerun.
