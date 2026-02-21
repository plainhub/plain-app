# Chat Message Data Structures and Flow (App + Web)

This document summarizes the current implementation-level chat data model and flow, including:

- Storage structures (`type = text / images / files`)
- How local DB messages change when App users send files
- What the receiving side does after a message arrives, and what data is updated
- Where files are downloaded from, and whether DB messages are updated after download
- Web-side send/receive flow, updated fields, and called APIs
- Practical example payloads

---

## 1. Core App Data Structures

### 1.1 Chat table (Room: `chats`)

Key `DChat` fields:

- `id: string`
- `fromId: string` (`me` / `local` / `peerId`)
- `toId: string` (`local` / `peerId`)
- `status: string` (`pending` / `sent` / `failed`)
- `content: DMessageContent` (actual message body)
- `createdAt`, `updatedAt`

### 1.2 Unified message wrapper

`DMessageContent`:

```json
{
  "type": "text | images | files",
  "value": "type-specific structure"
}
```

`type` enum:

- `text`
- `images`
- `files`

### 1.3 `text` structure

`DMessageText`:

```json
{
  "text": "hello",
  "linkPreviews": [
    {
      "url": "https://example.com",
      "title": "Example",
      "description": "...",
      "imageUrl": "https://...",
      "imageLocalPath": "fid:... or app://...",
      "imageWidth": 1200,
      "imageHeight": 630,
      "siteName": "Example",
      "domain": "example.com",
      "createdAt": "2026-02-21T10:00:00Z"
    }
  ]
}
```

### 1.4 `images` / `files` structure

Both use `value.items: DMessageFile[]`; only semantics differ (image vs file message).

`DMessageFile`:

```json
{
  "id": "item_id",
  "uri": "content://... | fid:<sha256> | fsid:<peer_file_id> | app://... | /abs/path",
  "size": 12345,
  "duration": 0,
  "width": 0,
  "height": 0,
  "summary": "",
  "fileName": "abc.jpg"
}
```

URI meaning:

- `fid:<sha256>`: local content-addressable storage (preferred persisted form for chat files)
- `fsid:<peer_file_id>`: remote peer file identifier (requires download)
- `content://...`: Android picker source URI (temporary placeholder during send flow)

---

## 2. App Send Flow (file-focused)

### 2.1 Sending text

1. Build `DMessageContent(type=text, value=DMessageText)`
2. `ChatHelper.sendAsync(...)` inserts a row into `chats`
   - Local chat: `status=sent`
   - Peer chat: `status=pending`
3. For peer chat, call `PeerChatHelper.sendToPeerAsync`
   - Success: update `status=sent`
   - Failure: update `status=failed`
4. Trigger async link preview fetching; when done, update the same message `content` and emit `MESSAGE_UPDATED`

### 2.2 Sending images/files (Android native UI)

#### Stage A: write a placeholder message immediately

After file selection, the app first creates placeholder `DMessageFile` entries (`uri=content://...`) and inserts a message:

- `type=images` or `type=files`
- `status=pending`
- Purpose: immediate UI rendering (thumbnail can be read from `content://`)

#### Stage B: import in background to content-addressable storage

Each file calls `ChatFileSaveHelper.importFromUri`:

1. Copy into a temp file
2. `AppFileStore.importFile(...)` deduplicates (weak hash + strong hash)
3. Returns `fid:<sha256>`

Then the message content is replaced with final `DMessageFile[]` (`uri` changes from `content://` to `fid:`), and status is updated:

- Sending to local: `status=sent`
- Sending to peer: starts as `pending`, then `sent` on success or `failed` on failure

#### Stage C: URI mapping when sending to peer

`PeerChatHelper.sendToPeerAsync` maps local `fid:`/local paths to `fsid:<peer_file_id>` for peer payload (`peer_file_id` is used by peer `/fs?id=...`).

> Note: local DB message content remains locally-resolvable (usually `fid:`). `fsid:` is the on-wire payload for peer delivery.

---

## 3. App Receive Flow (receiver side)

When a peer calls `/peer_graphql` `createChatItem`:

1. Server verifies signature + timestamp
2. Executes `ChatHelper.sendAsync(DChat.parseContent(content), fromId=peerId, toId="me")`
   - Message is inserted into `chats` first
3. If `text`: trigger link preview fetching; after completion, update message content and emit `MESSAGE_UPDATED`
4. If `images/files`:
   - Read `items` (typically `fsid:` URIs)
   - Enqueue each file via `DownloadQueue.addDownloadTask(...)`
5. Emit `MESSAGE_CREATED` to Web/other observers

---

## 4. Download Source and DB Updates After Download

### 4.1 Where file download happens

Download URL is built from peer info:

```text
https://{peer.ip}:{peer.port}/fs?id={peer_file_id}
```

`peer_file_id` comes from message `fsid:<peer_file_id>`.

### 4.2 What happens after download completes

`PeerFileDownloader.downloadAsync`:

1. Download into cache temp file
2. Validate downloaded size
3. Call `ChatFileSaveHelper.importDownloadedFile` and get `fid:<sha256>`
4. Update original message: file `uri` is changed from `fsid:...` to `fid:...`
   - via `chatDao.updateData(ChatItemDataUpdate(messageId, content))`
5. Emit `DownloadTaskDoneEvent`; UI layer then emits `MESSAGE_UPDATED`

Conclusion: **DB message content is updated after download** (`uri` changes).

---

## 5. Web Flow (send/receive, fields, APIs)

### 5.1 Web receives messages (realtime)

1. `App.vue` opens WebSocket and maps `EventType` to event names:
   - `1 -> message_created`
   - `2 -> message_deleted`
   - `3 -> message_updated`
   - `16 -> download_progress`
2. Events are dispatched through `emitter.emit(...)` to `ChatView.vue`
3. `ChatView` handlers:
   - `message_created`: dedupe then insert into Apollo cache (`chatItems`)
   - `message_updated`: update single `ChatItem` via `writeFragment`
   - `message_deleted`: `cache.evict`
   - `download_progress`: aggregate by `messageId` into `downloadProgress`

### 5.2 Web sends text

1. Add temporary local message `id=new_xxx` (optimistic)
2. Call GraphQL `sendChatItem(toId, content)`
3. On success, remove temp item; server message enters cache

Relevant fields:

- `content = {"type":"text","value":{"text":"..."}}`
- `fromId`, `toId`, `createdAt`, `id`

### 5.3 Web sends images/files

Two-stage flow:

#### Stage 1: local placeholder message

`ChatView.handleContentUpload` creates temp `ChatItem`:

- `id = new_xxx`
- `content.type = images|files`
- `content.value.items[].uri = original fileName`
- `data.ids = blob:` URLs for immediate preview

Then it inserts into Apollo cache.

#### Stage 2: send final chat message after upload

`useTasks` + `upload-queue`:

1. Upload through HTTP `/upload` or `/upload_chunk`
2. If `isAppFile=true`, server returns SHA-256 hash
3. After all uploads complete, build final message:
   - `uri = fid:<hash>`
4. Call GraphQL `sendChatItem(toId, content)`
5. Remove temp `new_xxx`; insert server-returned `ChatItem`

### 5.4 Web API summary

GraphQL:

- `query chatItems(id)`: fetch conversation
- `query peers`: fetch peer info
- `mutation sendChatItem(toId, content)`: send message
- `mutation deleteChatItem(id)`: delete message
- `query uploadedChunks(fileId)`: resumable upload state
- `mutation mergeChunks(fileId,totalChunks,path,replace,isAppFile)`: merge chunked upload

HTTP:

- `POST /upload`: direct upload
- `POST /upload_chunk`: chunk upload
- `GET /fs?id=...`: serve file (local file, `fid:`, thumbnail, etc.)
- `GET /proxyfs?id=...`: proxy peer `/fs` (certificate/CORS workaround)

WebSocket events:

- `message_created`
- `message_updated`
- `message_deleted`
- `download_progress`

---

## 6. Example Data

### 6.1 text message (`content` in DB)

```json
{
  "type": "text",
  "value": {
    "text": "Check this out https://example.com",
    "linkPreviews": [
      {
        "url": "https://example.com",
        "title": "Example Domain",
        "description": "Example Domain description",
        "imageUrl": "https://example.com/cover.jpg",
        "imageLocalPath": "fid:9f23ab...",
        "imageWidth": 1200,
        "imageHeight": 630,
        "siteName": "Example",
        "domain": "example.com",
        "createdAt": "2026-02-21T10:30:00Z"
      }
    ]
  }
}
```

### 6.2 images message (already persisted as `fid`)

```json
{
  "type": "images",
  "value": {
    "items": [
      {
        "id": "f1",
        "uri": "fid:7d4e1f0c9a...",
        "size": 203421,
        "duration": 0,
        "width": 1280,
        "height": 720,
        "summary": "",
        "fileName": "IMG_1001.jpg"
      }
    ]
  }
}
```

### 6.3 files message (received, not downloaded yet)

```json
{
  "type": "files",
  "value": {
    "items": [
      {
        "id": "f2",
        "uri": "fsid:q8gWm7...",
        "size": 5893021,
        "duration": 0,
        "width": 0,
        "height": 0,
        "summary": "",
        "fileName": "report.pdf"
      }
    ]
  }
}
```

### 6.4 same files message (after download)

```json
{
  "type": "files",
  "value": {
    "items": [
      {
        "id": "f2",
        "uri": "fid:1a2b3c4d5e...",
        "size": 5893021,
        "duration": 0,
        "width": 0,
        "height": 0,
        "summary": "",
        "fileName": "report.pdf"
      }
    ]
  }
}
```

### 6.5 Web `sendChatItem` (text) payload

```json
{
  "toId": "peer:peer_123",
  "content": "{\"type\":\"text\",\"value\":{\"text\":\"hello\"}}"
}
```

### 6.6 Web `sendChatItem` (files, post-upload) payload

```json
{
  "toId": "peer:peer_123",
  "content": "{\"type\":\"files\",\"value\":{\"items\":[{\"uri\":\"fid:7d4e1f0c9a...\",\"size\":203421,\"duration\":0,\"width\":0,\"height\":0,\"summary\":\"\",\"fileName\":\"notes.txt\"}]}}"
}
```

---

## 7. Direct Answers to Requirements

- When users send files, the App first stores a placeholder DB message (can be `content://`), then updates it to `fid:`.
- For peer delivery, final status depends on network send result (`sent` / `failed`).
- Receiver side auto-enqueues `files/images` for download.
- Download source is `https://peer_ip:peer_port/fs?id=<peer_file_id>`.
- After download, DB message is updated (`fsid -> fid`) and `MESSAGE_UPDATED` is emitted to Web/UI.
- Web file sending is a two-phase flow: local temp message + upload + `sendChatItem` final message replacing temp state.
