# Image search model packages

## Package contract

The contract and validation rules are defined by the shared Rust [manifest](../../plain-desktop/plain-rs/src/image_inference/manifest.rs). The default model and pinned download hashes have one source: [default_model.rs](../../plain-desktop/plain-rs/src/image_inference/default_model.rs).

Import `manifest.json` together with every file it declares through the image search settings. Encoders must be exported as paired ONNX models: image input float32, text inputs int64, and the selected embedding output float32 `[1, dimensions]`. Additional graph outputs are allowed. Hugging Face `tokenizer.json` supplies tokenization and special token processing. The manifest supplies fixed padding/truncation and image preprocessing. External ONNX tensor data files are not supported; export embedded weights.

A raw PyTorch checkpoint, a `.tflite` file, or an image encoder without its matching text encoder cannot be imported. Converting weights to ONNX does not change their license.

## Import behavior

Uploads are staged separately from the active package. Rust verifies file lengths and SHA-256 hashes, validates tokenizer settings and runs both encoders before activation. A failed validation keeps the previous package and index. Successfully changing the package fingerprint rebuilds embeddings; vectors from different models are never mixed.

The default activation ignores staged custom uploads; importing uses the separate `importImageSearchModel` mutation. Restoring uses the persisted active package. The ready view also has the import action, so changing models does not require deleting the active package first. Cancelling an upload does not activate a partial package. The default download is SigLIP2 Base under Apache-2.0. The App does not distribute or download MobileCLIP2 weights. Users importing other models must have permission for their intended use, including any restrictions on product development.

## Creating a custom package

Use `python3 scripts/package_image_search_model.py --help` for the package builder. Supply existing ONNX encoder files, their matching tokenizer, and the settings from the model's original preprocessing/export configuration. The builder copies inputs and generates the file declarations and hashes. Rust remains the final validator when importing.

Choose similarity thresholds using retrieval evaluation for that model; a threshold measured for another model is not interchangeable. Import support does not certify model accuracy. Validate query languages, representative photos and resource use before distributing a new default.

## Runtime builds

The shared Rust module uses the stable ONNX Runtime C API. Android builds ONNX Runtime from its pinned upstream source, packages it with the Rust library and NDK C++ runtime, and uses the same code for every distribution channel. It has no LiteRT SDK, LiteRT stubs or proprietary QNN runtime dependency.

The preparation script supports macOS, Linux, Windows and iOS targets. `PLAIN_ONNX_RUNTIME_DIR` selects the preparation output directory; `PLAIN_ONNX_SOURCE_BUILD=1` selects a source build on desktop targets. The preparation output includes upstream license and third-party notices; distribute these with the runtime. `PLAIN_ONNX_PROVIDER` selects an optional execution provider; the default is CPU, and initialization failures fall back to CPU. Acceleration requires per-model validation. Deployed desktop applications must package the native runtime beside their executable, or in the macOS bundle Resources directory. `PLAIN_ONNX_RUNTIME_LIBRARY` can override the desktop runtime path.

Platform album access and decoding of system-only image formats remain native bridges. The iOS PhotoKit bridge indexes only locally accessible images; cloud-only images are not downloaded by the indexer. Existing iOS media browsing and serving must be integrated before declaring complete iOS product support. Native inference portability alone does not establish that support.
