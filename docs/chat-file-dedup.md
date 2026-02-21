# Chat File Deduplication

## Problem

Sending the same file multiple times (or receiving the same file from multiple peers) creates redundant copies on disk, wasting storage.

## Solution: Content-Addressable File Store

All chat files are stored in a content-addressable store keyed by their SHA-256 hash. Identical files are stored only once, with a reference counter tracking how many chat messages point to each file.

### Storage Layout

```
{app external files dir}/
    ab/cd/abcd1234...   ← hash used as filename, split into 2-char subdirs
```

The real path is derived **deterministically** from the file ID (SHA-256 hex), so no database query is needed to resolve a path at display time.

### URI Scheme

Chat message files use the URI scheme `fid:{sha256hex}` instead of storing raw file system paths. The existing `app://` scheme remains for other non-chat files.

`fid:` is resolved to a real path by the `getFinalPath()` extension function, making it transparent to all display and API code.

## Deduplication Flow

When a file is added to the store (on send or after download), a two-step check is performed to balance speed and accuracy:

**Step 1 — Weak check (cheap)**
- Compute SHA-256 of the first 4 KB + last 4 KB of the file
- Query: `WHERE size = ? AND weak_hash = ?`
- No match → file is new, skip step 2

**Step 2 — Strong check (only on weak hit)**
- Compute full SHA-256 of the entire file
- Match → reuse the existing record, increment `ref_count`
- No match → insert new record and copy the file

## FileTable Schema

| Column | Description |
|--------|-------------|
| `id` | Full SHA-256 hex (64 chars), primary key |
| `size` | File size in bytes |
| `mime_type` | MIME type |
| `real_path` | Absolute path (for tooling/diagnostics only) |
| `ref_count` | Number of chat message items referencing this file |
| `weak_hash` | SHA-256 of edge bytes, used for the cheap first-pass lookup |

## Reference Counting

- **Import** (send or download): `ref_count` starts at 1. On a dedup hit it is incremented.
- **Delete message**: `ref_count` is decremented for each `fid:` file in the message. When it reaches 0 the physical file is deleted and the row is removed.

## HTTP File Serving (`/fs` API)

The `/fs` endpoint receives an encrypted `id` parameter. When decrypted it may be a `fid:` URI, which `getFinalPath()` resolves to the real path transparently. No special handling is required in the endpoint.
