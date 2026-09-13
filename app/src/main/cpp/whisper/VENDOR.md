# Vendored whisper.cpp

- Upstream: https://github.com/ggml-org/whisper.cpp
- Version: v1.9.4 (tag, cloned with submodules)
- Contents: `src/`, `include/`, `ggml/` (submodule), `LICENSE`, `README.md`
- Pruned at vendor time (CPU-only Android build, options stay OFF):
  `ggml/src/ggml-{opencl,cuda,vulkan,sycl,hexagon,metal,et,webgpu,openvino,cann,hip,musa,zdnn,zendnn,rpc,virtgpu,blas}`
- Build entry: `app/src/main/cpp/CMakeLists.txt` → `libwhisper_jni.so`
- Upgrade: re-clone the tag with submodules, re-apply the prune list, update
  `WHISPER_VERSION` in the CMakeLists and `WhisperModels` checksums if models change.
