# AI Image Search — Incremental Indexing

## Overview

The AI image search feature uses MobileCLIP-S2 to generate 512-dimensional embeddings for images, stored in Room (`image_embeddings` table). This document describes the incremental indexing architecture that keeps the index in sync with the device's photo library.

## Architecture

All index-related code lives in `app/.../plain/ai/`:

| File | Role |
|------|------|
| `ImageIndexManager.kt` | Central coordinator — serialized operation queue |
| `ImageMediaObserver.kt` | `ContentObserver` on `MediaStore.Images` |
| `ImageSearchIndexer.kt` | Embedding worker — single and parallel modes |
| `ImageSearchManager.kt` | Model lifecycle — triggers manager start/stop |
| `ImageSearchEvents.kt` | Event bus types for UI progress |

### Operation Flow

```
┌─────────────────────────────────────────────────┐
│             ImageIndexManager                    │
│  (serialized Channel<Op>, single consumer)       │
│                                                  │
│  Op.Add(ids)     → index specific images         │
│  Op.Remove(ids)  → delete embeddings from DB     │
│  Op.Sync         → diff MediaStore vs DB         │
│  Op.FullScan     → full rescan with progress UI  │
└─────────┬───────────────────────────────┬────────┘
          │                               │
          ▼                               ▼
  ImageSearchIndexer              ImageEmbeddingDao
  (embedding work)                (Room database)
```

All operations are enqueued into an **unbounded Channel** and processed **one at a time** by a single coroutine. This eliminates race conditions without explicit locking.

## Three Sync Strategies

### 1. Real-Time: ContentObserver

`ImageMediaObserver` monitors `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` for changes from any app (camera, gallery, file manager). Changes are **debounced** (1.5 seconds) to batch rapid notifications, then trigger `ImageIndexManager.enqueueSync()`.

- **Registered** when the AI model becomes READY (`ImageSearchManager.enable()` / `restoreIfEnabled()`)
- **Unregistered** when the model is disabled (`ImageSearchManager.disable()`)
- **Limitation**: Only works while the app is running

### 2. Diff Sync on Startup

When the model is restored on app startup, `ImageIndexManager.startup()` enqueues a `Sync` operation:

1. Query all image IDs from MediaStore
2. Query all embedding IDs from Room
3. Delete stale embeddings (IDs in Room but not in MediaStore)
4. Index new images (IDs in MediaStore but not in Room)

This catches any changes that occurred while the app was offline.

### 3. Direct Hooks for Known Operations

For operations initiated by the app itself, we hook directly for immediate response:

| Operation | Location | Hook |
|-----------|----------|------|
| Delete images | `MainGraphQL.deleteMediaItems` | `enqueueRemove(ids)` |
| Trash images | `MainGraphQL.trashMediaItems` | `enqueueRemove(ids)` |
| Restore from trash | `MainGraphQL.restoreMediaItems` | `enqueueAdd(ids)` |
| Delete from UI | `ImagesViewModel.delete()` | `enqueueRemove(ids)` |
| Upload / file scan | ContentObserver | `enqueueSync()` (automatic) |
| Full rescan (web UI) | `MainGraphQL.startImageIndex` | `fullScan(force)` |

## Lifecycle

```
App Start
  └→ ImageSearchManager.restoreIfEnabled()
       └→ loadModels()
       └→ ImageIndexManager.startup()
            ├→ Start processor coroutine
            ├→ Register ContentObserver
            └→ enqueueSync()  (startup diff)

User Enables AI Search
  └→ ImageSearchManager.enable()
       └→ download() if needed
       └→ loadModels()
       └→ ImageIndexManager.startup()

User Disables AI Search
  └→ ImageSearchManager.disable()
       └→ ImageIndexManager.shutdown()
            ├→ Unregister ContentObserver
            └→ Cancel processor coroutine
       └→ Close models, delete files, clear DB
```

## Indexing Modes

### Incremental (single worker)

Used by `ImageIndexManager` for small batches (add, sync with few new images):

```
ImageSearchIndexer.indexImages(images)
  → Single ImageEmbedWorker
  → Batch insert (20 at a time)
  → No progress UI events
```

### Full Scan (parallel workers)

Used for user-triggered rescan via `startImageIndex` mutation:

```
ImageSearchIndexer.start(forceReindex)
  → 3 parallel bitmap loaders (Dispatchers.IO)
  → 4 parallel inference workers (Dispatchers.Default)
  → Batch writer with ImageIndexProgressEvent
  → Stale cleanup + new image indexing
```

## Integration with scanFileByConnection

`scanFileByConnection` registers files with MediaStore after upload/download. The ContentObserver approach means **no changes to `scanFileByConnection` call sites are needed**:

1. File is uploaded → `scanFileByConnection(file)` updates MediaStore
2. MediaStore update triggers `ContentObserver.onChange()`
3. Debounced → `ImageIndexManager.enqueueSync()`
4. Sync diffs IDs → indexes the new image

This keeps upload code decoupled from AI indexing.

## Thread Safety

- `ImageIndexManager` uses a **Channel** for serial processing — no concurrent ops
- `ImageSearchIndexer.isRunning` and `cancelled` are `@Volatile`
- `indexedImages` counter uses `AtomicInteger` in parallel mode
- `ContentObserver` debouncing uses coroutine cancellation (thread-safe)

## Database Schema

```sql
CREATE TABLE image_embeddings (
    id   TEXT PRIMARY KEY,   -- MediaStore image ID
    path TEXT NOT NULL,      -- File path (for lazy cleanup)
    embedding BLOB NOT NULL  -- Float32[512] as bytes
);
```

Key DAO operations:
- `insertAll(List<DImageEmbedding>)` — REPLACE strategy (upsert)
- `getAllIds()` — For diff comparison
- `deleteByIds(List<String>)` — Prune stale entries
- `deleteAll()` — Force reindex cleanup
