# AI Image Search

On-device semantic image search powered by MobileCLIP-S2 (Apple). Users type natural-language queries (e.g. "sunset on the beach") and the system returns photos ranked by visual relevance — all processing runs locally on Android via LiteRT, no cloud API.

## Model

MobileCLIP-S2 (`datacomp` pretrained), converted from PyTorch to TFLite via `ai-edge-torch`. Input: 256×256 px. Embedding: 512-d float32. Image encoder ~200 MB, text encoder ~200 MB, tokenizer ~2.2 MB. Max 77 tokens. Normalization: scale pixels to `[0,1]` only (no CLIP mean/std). Files are hosted on HuggingFace (`plainhub/mobileclip-s2-tflite`).

## Backend Design (Android)

- **State machine**: `UNAVAILABLE → DOWNLOADING → LOADING → READY` (plus `ERROR`). Persisted across app restarts.
- **Model download**: Fetches 3 files (image TFLite, text TFLite, tokenizer.json) from HuggingFace. Supports cancellation.
- **Photo indexing**: Background job iterates MediaStore photos, skips already-indexed ones, encodes each to a 512-d embedding, stores in Room DB. Supports cancellation. Emits batch progress events.
- **Search**: Encodes query text to embedding, computes dot product against all stored image embeddings, returns results above a minimum relevance threshold (0.15).
- **Image embedding**: Load → EXIF rotation → center crop → NCHW float32 → TFLite → 512-d vector.
- **Text embedding**: CLIPTokenizer (byte-level BPE, vocab from `tokenizer.json`) → int64 token IDs → TFLite → L2-normalized 512-d vector.
- **Storage**: Room table (`image_embeddings`) with image ID, file path, float[512] blob, timestamp.

## GraphQL API

- `enableAIImageSearch` — download models if needed, load, persist enabled state.
- `disableAIImageSearch` — unload models, delete files and DB entries.
- `cancelAIImageSearchDownload` — cancel in-progress download.
- `startAIImageIndex(force)` — start background photo indexing; `force=true` re-indexes all.
- `cancelAIImageIndex` — stop background indexing.
- `imageSearchStatus` — polling query returning current status, download progress, index counts.
- `images(query)` — when query has `text:` prefix and status is READY, performs semantic search.

## Frontend Design (Web)

A modal accessible from the images view. Behavior by state:

- **UNAVAILABLE / ERROR**: Setup screen — download button (auto-fetches from server, which proxies HuggingFace), or manual upload of the 3 model files.
- **DOWNLOADING**: Progress bar with cancel option.
- **LOADING**: Spinner.
- **READY, not indexed**: Button to start indexing.
- **READY, indexing**: Progress bar with stop option.
- **READY, indexed**: Shows count; rescan button; danger zone to remove model (inline confirm — no browser dialogs).

All API-triggering buttons show a loading spinner until the next status update arrives.

