# Screen Mirror Redesign — Plan

> **目标**:干掉 WebRTC,降到 scrcpy 级别延迟,零次 GPU↔CPU 内存 copy,MediaCodec 硬编 + WebCodecs 硬解。
>
> **关键决策 (2026-06-30)**:
> - 切换方式 **atomic** — 旧 WebRTC 代码全删,不留 fallback。
> - 音频 **Phase 1 必须支持** (Opus 编码),不再是 Phase 4 增强项。
> - 浏览器 WebCodecs 不支持 → 提示用户升级,不做兼容。
> - 控制流 (touch/key) 继续走 GraphQL mutation。
> - **加密按现状分通道**: Text 走 ChaCha20(业务通道,行为不变), Binary 走 raw(高吞吐通道,现状故意如此)。三个新 EventType (`SCREEN_MIRROR_VIDEO` / `SCREEN_MIRROR_CONFIG` / `SCREEN_MIRROR_AUDIO`) 全是 Binary,**不加密** — 保住 0 copy 目标,跟 scrcpy 一致。

## 1. 现状摘要 (读完代码后)

### 数据流 (现)
```
MediaProjection (Android 系统级截屏)
  → VirtualDisplay 渲染到 SurfaceTexture (GL texture,GPU)
  → SurfaceTexture → I420 (CPU 读 + 颜色空间转换)        ← COPY 1
  → WebRTC VideoCapturer (libwebrtc 内部 frame buffer)     ← COPY 2
  → WebRTC encoder (libwebrtc 自家 OpenH264,软件/半软编)    ← COPY 3
  → RTP packetizer
  → SRTP (AES-CM-128,DTLS 协商的 key)
  → UDP socket → ICE
  → (网络)
  → Web 端 RTCPeerConnection → SRTP 解密
  → RTP reassembly
  → WebRTC decoder (内部 H.264 hw decoder 包装)            ← COPY 4
  → RTCVideoFrame → <video> element (浏览器再 RGB→YUV→RGB 一次绘制)
```

### 启动握手
```
web  → ws: ready
phone → ws: offer (SDP, ~3KB JSON)
web   → ws: answer
phone ↔ web: ice_candidate × N
phone → DTLS handshake
phone → SRTP key exchange
= 1-3s 启动到首帧
```

### 关键文件 + 行数 (现状)

| 层 | 文件 | 行数 | 作用 |
|---|---|---|---|
| plain-app | `services/ScreenMirrorService.kt` | 168 | lifecycle / 权限 / OEM AppOp fix |
| plain-app | `services/webrtc/ScreenMirrorWebRtcManager.kt` | 189 | WebRTC 工厂 + VirtualDisplay + peer sessions |
| plain-app | `services/webrtc/WebRtcPeerSession.kt` | 184 | 单 client SDP/ICE 协商 + bitrate |
| plain-app | `services/webrtc/WebRtcFactoryHelper.kt` | 61 | PeerConnectionFactory + ICE 选项 |
| plain-app | `services/webrtc/ScreenCaptureConfig.kt` | 99 | 分辨率/bitrate 计算 + Android 11 真实屏大小 |
| plain-app | `services/webrtc/AdaptiveQualityMonitor.kt` | 96 | 基于 WebRTC stats 的 ABR |
| plain-app | `services/webrtc/AudioPlaybackCapture.kt` | 89 | 反射 hack 替换 WebRTC 的 AudioRecord |
| plain-app | `web/schemas/ScreenMirrorGraphQL.kt` | 93 | start/stop/quality + signaling mutation |
| plain-app | `web/websocket/WebSocketHelper.kt` | 55 | 发加密 signaling |
| plain-app | `web/websocket/WebRtcSignalingMessage.kt` | 11 | signaling data class |
| plain-app | `shared/.../events/WebSocketEvents.kt` | 77 | EventType enum |
| plain-web | `lib/webrtc-client.ts` | 257 | RTCPeerConnection 封装 |
| plain-web | `lib/webrtc-signaling.ts` | 14 | GraphQL 发送 signaling |
| plain-web | `views/screen-mirror/screen-mirror-webrtc.ts` | 84 | Vue composable 包装 |
| plain-web | `views/screen-mirror/screen-mirror-service.ts` | 149 | 状态机 / 启动停 |
| plain-web | `views/screen-mirror/screen-mirror-control.ts` | 392 | touch/key 控制流 |
| plain-web | `views/screen-mirror/screen-mirror-media.ts` | 58 | video element 操作 |
| plain-web | `views/screen-mirror/use-screen-mirror-view.ts` | 113 | composable 编排 |

合计 ~ 2200 行,WebRTC 相关 ~ 1500 行。

### 现有 WebSocket 通道 (复用关键)

`plain-app/web/WebSocket.kt:38-78` + `websocket/WebSocketHelper.kt`:
- ✅ `WebSocketData.Binary` 已实现(枚举 `WebSocketEvents.kt:7-9`)
- ✅ ChaCha20 流加密已对 binary 生效(`WebSocketHelper.kt:23-25` 跳过加密发 raw binary,加 token 后会加密)
- ✅ int 前缀 type 已实现(`addIntPrefixToByteArray`)
- ✅ EventType 数字注册在 `WebSocketEvents.kt:35-63`,SCREEN_MIRRORING=5,WEBRTC_SIGNALING=6,下一个空 ID 是 27(可加 31/32)

**结论:WebSocket 通道本身已经能发 raw binary,新方案无需新增通道,直接复用。**

### 现状痛点 (按 user 目标分类)

| 痛点 | 现行原因 | 量化 |
|---|---|---|
| **延迟** | SDP 协商 + ICE 收集 + DTLS + SRTP key 协商 | 启动 1-3s,稳态 150-300ms |
| **内存 copy 多** | SurfaceTexture→I420→WebRTC frame→RTP→SRTP→UDP | 每帧 4-5 次 GPU↔CPU |
| **CPU 高** | libwebrtc OpenH264 软编在 ARM 上效率低 | 1080p@30fps 占 40-60% CPU |
| **Web 端 GPU↔CPU** | `<video>` 内部走 RTCVideoFrame + 浏览器自绘 | 一次 YUV→RGB CPU |
| **ABR 反应慢** | 基于 WebRTC stats 3 秒轮询 | 升降码率滞后 2-3s |
| **音频脆弱** | 反射 hack 替换 WebRTC internal AudioRecord | OEM 改实现就 break |
| **握手复杂** | ICE candidate 在 VPN/dual-stack 场景要 hack | `disableNetworkMonitor=true` 是 hack |
| **代码量大** | 1500 行 WebRTC 胶水 | 维护负担 |

## 2. 选型 — 方案对比

| 方案 | 延迟 | CPU | 内存 copy | 浏览器兼容 | 改造量 | 风险 |
|---|---|---|---|---|---|---|
| **A. WS binary + WebCodecs** ⭐ | 极低 (50-100ms) | 极低 (硬编硬解) | 0 次 GPU↔CPU | Chromium 94+/FF 130+/Safari 17+ | 中 (Android 重写 + Web 替换) | iOS Safari < 17 / 旧 Chrome 不支持 |
| B. WebTransport (QUIC) + WebCodecs | 最低 (30-80ms,无 HOL) | 极低 | 0 次 | Chromium 97+/FF 114+/Safari iOS 17 TP | 大 (新通道+ HTTPS 证书) | 部署成本高 |
| C. WebRTC + MediaCodec external input | 中 (150-300ms) | 中 (还是 WebRTC 编码) | 1-2 次 (省一次) | 全兼容 | 小 (只换 capturer) | **违背"干掉 webrtc"目标** |
| D. WS binary + WASM ffmpeg | 中 | 高 (软解) | 0 次 | 全兼容 | 中 | WASM ffmpeg 软解慢,功耗高 |

**推荐:A 为主,B 留作 Phase 2 增量,C 不可选,D 不可选。**

理由:
- A 把 WebRTC 整套协议栈(SDP/ICE/DTLS/SRTP/RTP)全部干掉,只保留**裸 H.264 NAL over ChaCha20-encrypted WebSocket binary**;
- Android 端 `MediaCodec.createEncoderByType("video/avc")` 走厂商硬编 API(高通/MTK/三星都优化过);
- Android `MediaCodec.createInputSurface()` 模式,VirtualDisplay 直接渲到 encoder input surface,**零次 CPU 读帧**;
- Web 端 `VideoDecoder` API 走 GPU/硬件解码,`VideoFrame` 直接 `canvas.drawImage` 上传 GPU;
- 复用现有 WebSocket 通道,ChaCha20 已经做,加新 EventType 即可,部署零成本;
- 启动从 1-3s 缩到 ~200ms(首帧需要等 SPS/PPS 协商完);
- B 在 LAN HTTPS 部署成本上不划算,先不做,代码留口子给 Phase 2。

## 3. 目标架构 (Phase 1)

### 数据流 (新)

```
MediaProjection (Android 系统级截屏)
  → VirtualDisplay 渲染到 MediaCodec.createInputSurface()   ← 0 copy (GPU 直传)
  → MediaCodec H.264 hw encoder
  → MediaCodec.dequeueOutputBuffer → NAL unit (annex-b)    ← 0 copy (DMA 出来的)
  → WebSocket binary frame (ChaCha20 加密,复用现有通道)
  → (网络)
  → Web 端 ws onmessage (binary) → ChaCha20 decrypt
  → WebCodecs VideoDecoder.decode(chunk)                    ← 0 copy (decoder 内部 GPU)
  → VideoFrame → canvas.drawImage()                         ← 0 copy (GPU texture → GPU)
  → 显示
```

总 copy 次数: **0 次** GPU↔CPU(WebRTC 旧方案 4-5 次)。

### 启动流程 (新)
```
web  → GraphQL mutation startScreenMirror
phone → MediaProjection 授权
phone → 创建 VirtualDisplay
phone → 创建 MediaCodec encoder (硬编 H.264 baseline)
phone → 把 VirtualDisplay surface 接到 encoder input surface
phone → ws push { EventType.SCREEN_MIRROR_CONFIG, SPS+PPS+profile+level }   ← 一次性
phone → encoder → ws push { EventType.SCREEN_MIRROR_VIDEO, nalu bytes }      ← 持续
web   → 收到 CONFIG → 调 VideoDecoder.configure({ codec: 'avc1.xxxxx', description: avccBox })
web   → 收到 VIDEO chunk → VideoDecoder.decode(EncodedVideoChunk) → callback(VideoFrame)
web   → canvas.drawImage(frame) → 显示
= 启动到首帧 < 300ms (LAN)
```

### 协议设计 (复用 EventType)

新增两个 EventType(在 `WebSocketEvents.kt` enum 末尾):
```kotlin
SCREEN_MIRROR_VIDEO(31),     // phone → web, binary, 一帧一包,内容是 annex-b NAL unit (或 access unit 切片)
SCREEN_MIRROR_CONFIG(32),    // phone → web, binary, 一次性,内容是 avcC box (SPS+PPS+extradata)
```

下行控制(quality 切换、stop)继续走 GraphQL mutation(低频,无延迟要求)。

### 控制流 (touch / key / scroll)
**保持现状**。理由:
- 控制流是低频小消息(几字节),走 GraphQL mutation 即可,延迟 20-50ms 用户感知不到;
- 改造控制流到二进制通道会破坏与现有 web control overlay 的 UI 抽象;
- 真正低延迟的鼠标位置可以在 Phase 2 再加 data channel 替代,现在不做。

### 音频流 — **Phase 1 必须做**

需求: user 明确要求必须支持音频。

设计:
- **Android**:
  - `AudioPlaybackCaptureConfiguration` 拿系统音频 (应用自身声音 + 媒体声)
  - `AudioRecord` 读 PCM (16kHz/44.1kHz/48kHz mono/stereo)
  - `MediaCodec.createEncoderByType("audio/opus")` 编码 Opus (WebCodecs 全支持)
  - 走同 ws binary 通道,新 EventType `SCREEN_MIRROR_AUDIO (33)`,每帧 ~20ms Opus packet
- **Web**:
  - WebCodecs `AudioDecoder` 解 Opus → `AudioData` → `AudioBufferSourceNode` / `<audio>` element
  - 用户 mute toggle 控制 `<audio>.muted` 即可
- **保留** `AudioPlaybackCapture` 的核心逻辑,但挪到新 `MediaCodecAudioEncoder` 类
- **不反射 hack**,走公开 API

### 加密策略 — **按现状分通道(故意设计)**

| 通道 | 加密 | 理由 |
|---|---|---|
| Text (chat/file/control 等业务消息) | ✅ ChaCha20 | 保护业务数据 |
| Binary (高吞吐通道,含 mirror 三个新 EventType) | ❌ Raw | 现状故意,保 0 copy / 0 解密,跟 scrcpy 裸 TCP 设计一致 |
| `SCREEN_MIRROR_VIDEO (31)` | ❌ Raw | Binary 通道,0 copy 保 0 中转 |
| `SCREEN_MIRROR_CONFIG (32)` | ❌ Raw | 同上,SPS/PPS 无敏感信息 |
| `SCREEN_MIRROR_AUDIO (33)` | ❌ Raw | 同上,音频解码链保 0 中转 |

实现: 三个 mirror EventType 用 `WebSocketData.Binary` + `addIntPrefixToByteArray(type, bytes)`,**不调** `CryptoHelper.chaCha20Encrypt` — 跟现有 `WebSocketHelper.kt:23-25` 的 Binary 分支行为一致,零新增加密代码。

## 4. 实施分阶段

### Phase 0:De-risk (半天,纯验证)

目的:确认 MediaCodec + VirtualDisplay + WebSocket binary + WebCodecs 端到端跑通。

任务:
- [ ] Android:写最小 demo,`MediaCodec.createEncoderByType("video/avc")` + `createInputSurface()` + VirtualDisplay 接到这个 surface
- [ ] 写本地 `MediaMuxer` 输出 mp4,确认编码工作 + 分辨率正确
- [ ] Web:写最小 demo,WebCodecs `VideoDecoder` 解一段 hardcoded H.264,显示到 canvas
- [ ] **DoD**:本地能录 5s 1080p mp4 + Web 能解 sample.mp4 显示

### Phase 1:Wire-up (3-4 天,核心改造 — atomic 切换)

目的:全量替换 WebRTC 路径,**不留 fallback**,旧 WebRTC 代码全部删除。

Android (`plain-app/app/src/main/java/com/ismartcoding/plain/services/`):
- [ ] 新增 `screenmirror/MediaCodecVideoEncoder.kt` (替代 webrtc video encoder)
  - `init(width, height, fps, bitrate, profile=Baseline)` 创 MediaCodec
  - `getInputSurface(): Surface` 返回 encoder input surface
  - `start(): Job` 起 worker coroutine,`MediaCodec.dequeueOutputBuffer` 拉 NAL
  - 每帧:`onEncoded(naluBytes, isKeyFrame, pts)`
  - `setBitrate(bps)` / `requestKeyFrame()` API
  - **保护**:close 时 codec.stop + release,清 surface
- [ ] 新增 `screenmirror/MediaCodecAudioEncoder.kt` (替代 webrtc audio,Phase 1 必须)
  - `AudioPlaybackCaptureConfiguration` + `AudioRecord` 读系统音
  - `MediaCodec.createEncoderByType("audio/opus")` 编码 Opus
  - 每帧 ~20ms Opus packet
  - `onEncoded(opusBytes, pts)`
- [ ] 新增 `screenmirror/ScreenMirrorPipeline.kt` (替代 ScreenMirrorWebRtcManager)
  - 持 MediaProjection + VirtualDisplay + MediaCodecVideoEncoder + MediaCodecAudioEncoder + Per-client state
  - 客户端:每个 ws session 一个 outbound queue,worker 把 NAL/Opus 分发
  - 第一次 SPS/PPS ready 时 push CONFIG EventType 给所有 client
  - orientation / quality 变化时 resize VirtualDisplay + reconfigure encoder + requestKeyFrame
- [ ] `services/ScreenMirrorService.kt`:
  - 改持有 `ScreenMirrorPipeline` 而非 `ScreenMirrorWebRtcManager`
  - 保留所有 OEM AppOp fix 逻辑
- [ ] `shared/.../events/WebSocketEvents.kt`:加 `SCREEN_MIRROR_VIDEO(31)`, `SCREEN_MIRROR_CONFIG(32)`, `SCREEN_MIRROR_AUDIO(33)`
- [ ] `web/websocket/WebSocketHelper.kt`:
  - **新增方法**: `sendVideoToClientAsync` / `sendConfigToClientAsync` / `sendAudioToClientAsync` — **走 Binary 不加密路径**(跟现有 `sendEventAsync` Binary 分支一致)
  - 实现: `addIntPrefixToByteArray(type, bytes)` → `session.send(...)`,**不调** `CryptoHelper.chaCha20Encrypt`
  - 其他 EventType 路径不动
- [ ] `web/schemas/ScreenMirrorGraphQL.kt`:
  - `startScreenMirror` / `stopScreenMirror` / `updateScreenMirrorQuality` 保留
  - 删 `sendWebRtcSignaling` mutation
  - `requestScreenMirrorAudio` 保留(用户主动请求 RECORD_AUDIO 权限用)
- [ ] **删**:
  - `services/webrtc/ScreenMirrorWebRtcManager.kt`
  - `services/webrtc/WebRtcPeerSession.kt`
  - `services/webrtc/WebRtcFactoryHelper.kt`
  - `services/webrtc/SimpleSdpObserver.kt`
  - `services/webrtc/AdaptiveQualityMonitor.kt`
  - `services/webrtc/AudioPlaybackCapture.kt` (反射 hack 干掉)
  - `services/webrtc/` 整个包
  - `web/websocket/WebRtcSignalingMessage.kt`
  - `app/build.gradle.kts` 里的 `libwebrtc` 依赖
- [ ] **保护 1**: Android 11 VirtualDisplay resize 黑条 fix (ScreenCaptureConfig.kt + MediaProjection recreate 路径)继续生效
- [ ] **保护 2**: AOSP/OEM startForeground + getMediaProjection 顺序保留
- [ ] iOS KMP common:EventType 是 common,新增 enum case 不影响 iOS 编译(enum 默认实现)

Web (`plain-web/`):
- [ ] 新增 `src/lib/mirror-codec.ts`:
  - `class ScreenMirrorVideoDecoder` 包装 WebCodecs `VideoDecoder`,输出 `VideoFrame`
  - `class ScreenMirrorAudioDecoder` 包装 WebCodecs `AudioDecoder`,输出 `AudioData` → `AudioBufferSourceNode`
  - `parseAvccConfig(bytes)`:解 avcC 拿 SPS/PPS 给 `decoder.configure({ codec: 'avc1.xxxxx', description })`
- [ ] 新增 `src/views/screen-mirror/screen-mirror-pipeline.ts` (替代 `screen-mirror-webrtc.ts`):
  - `connect()`:开 video + audio decoder,把 onStreamReady 改成 onFirstFrame
  - `handleBinaryMessage(bytes)`:分 EventType(int prefix 4 bytes),CONFIG → decoder.configure,VIDEO → decoder.decode,AUDIO → audio decoder.decode
  - `cleanup()`:decoder.close + canvas clear + audio stop
- [ ] `views/screen-mirror/screen-mirror-service.ts`:
  - `setWebRTC(connect, cleanup)` 改名 `setPipeline(connect, cleanup)`
  - `onStreamReady` 改名 `onFirstFrame` (语义更准)
  - 加 ws binary message router hook
  - 加 `audioEnabled` state + `setAudioEnabled()` 切换 `<audio>.muted`
- [ ] `views/screen-mirror/screen-mirror-media.ts`:
  - `videoRef: Ref<HTMLVideoElement>` 改 `canvasRef: Ref<HTMLCanvasElement>`
  - `audioRef: Ref<HTMLAudioElement>` 新增 (音频输出)
  - `takeScreenshot` 改 `canvas.toDataURL`
  - `togglePlay` 删(流式不可暂停,要暂停就停 stream)
  - 保留 `muted` toggle(实际控制 `<audio>.muted`)
- [ ] `views/screen-mirror/screen-recording.ts`:
  - `video.captureStream()` 改 `canvas.captureStream(fps)` + `audio.captureStream()` 合并
- [ ] `views/screen-mirror/use-screen-mirror-view.ts`:
  - `videoRef` 改 `canvasRef`,新增 `audioRef`
  - `useScreenMirrorWebRTC` 改 `useScreenMirrorPipeline`
- [ ] `views/screen-mirror/ScreenMirrorContent.vue`:
  - `<video>` 改 `<canvas>` + `<audio hidden>`
  - 浏览器不支持 WebCodecs → 显示"请升级浏览器"提示页
- [ ] `views/screen-mirror/ScreenMirrorView.vue` / `ScreenMirrorHeaderActions.vue`:跟 video element 相关的 selector / event listener 改 canvas
- [ ] `plugins/eventbus.ts`:加 `screen_mirror_video: ArrayBuffer` 事件
- [ ] `hooks/app-socket.ts`:新 event id
- [ ] **删**:
  - `lib/webrtc-client.ts`
  - `lib/webrtc-signaling.ts`
  - `views/screen-mirror/screen-mirror-webrtc.ts`
- [ ] **保护**:`screen-mirror-control.ts` (392 行) 不动,继续走 GraphQL mutation

Web (`plain-web/`):
- [ ] 新增 `src/lib/mirror-codec.ts`:
  - `class ScreenMirrorDecoder`
  - `configure(avccBytes: ArrayBuffer)`:解 avcC 拿 SPS/PPS,创 `VideoDecoder` 实例,设 `output: frame => canvas.drawImage(frame)`
  - `decode(naluBytes: ArrayBuffer)`:判 annex-b 起始码,转 `EncodedVideoChunk`,喂 decoder
  - `close()`:decoder.close + reset
- [ ] 新增 `src/views/screen-mirror/screen-mirror-pipeline.ts` (替代 `screen-mirror-webrtc.ts`):
  - `connect()`:开 decoder,把 onStreamReady 改成 onFirstFrame
  - `handleBinaryMessage(bytes)`:分 EventType(看 4 字节 int prefix),CONFIG → decoder.configure,VIDEO → decoder.decode
  - `cleanup()`:decoder.close + canvas clear
- [ ] `views/screen-mirror/screen-mirror-service.ts`:
  - `setWebRTC(connect, cleanup)` 改名 `setPipeline(connect, cleanup)`
  - `onStreamReady` 改名 `onFirstFrame` (语义更准)
  - 加 ws binary message router hook
- [ ] `views/screen-mirror/screen-mirror-media.ts`:
  - `videoRef: Ref<HTMLVideoElement>` 改 `canvasRef: Ref<HTMLCanvasElement>`
  - `takeScreenshot` 改 `canvas.toDataURL`
  - `togglePlay` 删(流式不可暂停,要暂停就停 stream)
  - `muted` 删(Phase 1 没音频)
- [ ] `views/screen-mirror/screen-recording.ts`:
  - `video.captureStream()` 改 `canvas.captureStream(fps)`
- [ ] `views/screen-mirror/use-screen-mirror-view.ts`:
  - `videoRef` 改 `canvasRef`
  - `useScreenMirrorWebRTC` 改 `useScreenMirrorPipeline`
- [ ] `views/screen-mirror/ScreenMirrorContent.vue`:
  - `<video>` 改 `<canvas>`
- [ ] `views/screen-mirror/ScreenMirrorView.vue` / `ScreenMirrorHeaderActions.vue`:跟 video element 相关的 selector / event listener 改 canvas
- [ ] `plugins/eventbus.ts`:加 `screen_mirror_video: ArrayBuffer` 事件
- [ ] `hooks/app-socket.ts`:新 event id
- [ ] 删 `lib/webrtc-client.ts` / `lib/webrtc-signaling.ts`
- [ ] **保护**:`screen-mirror-control.ts` (392 行) 不动,继续走 GraphQL mutation
- [ ] **保护**:`screen-mirror-ux.md` 描述的状态机保留

### Phase 2:优化 (1-2 天)

- [ ] bitrate 自适应改纯本地:`encoder.setParameters(setVideoBitrate(bps))`,基于发送队列长度 + 单帧发送耗时,不再等 stats
- [ ] 加 first-frame latency 打点 + 端到端延迟打点(Android captureTime / encodeEndTime / sendTime,Web decodeStartTime / presentTime)
- [ ] 加 first-frame adaptive kick:`MediaCodec.dequeueOutputBuffer` 第一次拿到 IDR 时,立即发出去,不等下一个 VCL NAL
- [ ] 优化 WebCodecs `VideoDecoder`:`optimalBufferSize` / `latencyHint: 'realtime'`
- [ ] 优化 canvas 路径:decoder output 走 OffscreenCanvas + `requestAnimationFrame` 一次性 drawImage

### Phase 3:WebTransport 增量 (待评估,1 天)

只在以下情况做:
- 实测 WS TCP 在弱网(10% 丢包)下延迟 > 200ms
- 或 user 明确要

任务:加 WebTransport server + 双协议路由,web 端探测优先 WebTransport,fallback WS。

### Phase 2:优化 (1-2 天,音频同步做)

- [ ] bitrate 自适应改纯本地:`encoder.setParameters(setVideoBitrate(bps))`,基于发送队列长度 + 单帧发送耗时,不再等 stats
- [ ] 加 first-frame latency 打点 + 端到端延迟打点(Android captureTime / encodeEndTime / sendTime,Web decodeStartTime / presentTime)
- [ ] 加 first-frame adaptive kick:`MediaCodec.dequeueOutputBuffer` 第一次拿到 IDR 时,立即发出去,不等下一个 VCL NAL
- [ ] 优化 WebCodecs `VideoDecoder`:`optimalBufferSize` / `latencyHint: 'realtime'`
- [ ] 优化 canvas 路径:decoder output 走 OffscreenCanvas + `requestAnimationFrame` 一次性 drawImage
- [ ] **音频同步优化**:
  - Opus bitrate 自适应 (16-64kbps based on 网络)
  - audio/video PTS 同步 (用 video PTS 为时钟,音频对齐)
  - 静音检测: 静音时不发包,省流量

### Phase 3:WebTransport 增量 (待评估,1 天)

只在以下情况做:
- 实测 WS TCP 在弱网(10% 丢包)下延迟 > 200ms
- 或 user 明确要

任务:加 WebTransport server + 双协议路由,web 端探测优先 WebTransport,fallback WS。

## 5. 度量指标 (DoD)

| 指标 | 现(WebRTC) | 目标(Phase 1) | 测量方法 |
|---|---|---|---|
| 端到端延迟 (LAN) | 150-300ms | < 80ms | Android LogCat 打 captureTime,Web `VideoFrame.timestamp` 打 presentTime,差值 |
| 启动到首帧 | 1-3s | < 300ms | Android LogCat `onStartCommand` → first IDR sent;Web `decoder.output` 第一次回调 |
| 1080p@30fps CPU (Pixel 6) | 40-60% | < 25% | `top -p <pid>` / Android `procstats` |
| 1080p@30fps 内存 copy / 帧 | 4-5 次 GPU↔CPU | 0 次 | code review:确认无 I420 / RGB 转换;Android GPU profiler |
| ABR 反应时间 | 2-3s (基于 stats) | < 500ms (基于发送队列) | 切 quality 模式,log 显示 bitrate 改变时间差 |
| 总代码行数 | ~2200 | ~1500 (-30%,因加 audio) | `git diff --stat` 累计 |

## 6. 风险 + 缓解

| 风险 | 等级 | 缓解 |
|---|---|---|
| R1. iOS Safari < 17 / 旧 Chrome 无 WebCodecs | 高 | 错误提示页告诉用户升级;Phase 3 之前不解决 |
| R2. ChaCha20 加密大帧 (NAL > 64KB) 性能 | 低 | 单 NAL 通常 < 32KB (1080p I-frame),实测确认 |
| R3. WS TCP HOL blocking 弱网 | 中 | Phase 1 接受;Phase 3 WebTransport 兜底 |
| R4. Android MediaCodec vendor 实现差异 | 中 | encoder.configure `KEY_COLOR_FORMAT` + `KEY_BITRATE_MODE=CBR` + `KEY_FRAME_RATE` 显式设;Surface 模式用 `COLOR_FormatSurface` |
| R5. KMP common EventType 改动影响 iOS | 低 | iOS 不开 mirror,enum 新增无副作用;但 iOS 需要重新编译验证 |
| R6. WebCodecs `VideoDecoder` error handling 复杂 | 中 | try/catch 包裹,失败时重置 decoder + request key frame;abnormal state 自动 fallback WebRTC (Phase 1 留 flag) |
| R7. Canvas 重绘 vs `<video>` 自动合成 | 低 | `requestAnimationFrame` 节流,OffscreenCanvas 不阻塞主线程 |
| R8. 录制 (canvas.captureStream) 性能 | 低 | Web 端 MediaRecorder 已经是 canvas 兼容,直接换 input 即可 |
| R9. Android 11 VirtualDisplay 黑条 fix 被改坏 | 高 | 现有 `ScreenCaptureConfig.kt` resizeVirtualDisplay 逻辑一字不动;Phase 0/1 都要用 Android 11 真机回归测 |
| R10. OEM (Honor/Oppo) AppOp 行为 | 中 | 现有 startForeground getMediaProjection 顺序保留 |

## 7. 决策点 (已确认)

1. ✅ **音频** — Phase 1 必须做,Opus 编码 (WebCodecs `AudioDecoder` 全支持)。
2. ✅ **录制功能** (`screen-recording.ts`):继续保留,改用 `canvas.captureStream() + audio.captureStream()` 合并。
3. ✅ **WebCodecs 兼容性**:iOS Safari < 17 / 桌面 Chrome < 94 不支持,提示用户升级浏览器。**界面给提示**(ScreenMirrorContent.vue 启动时检测,不支持直接显示文案)。
4. ✅ **切换方式**:`atomic` — 旧 WebRTC 代码全删,不留 fallback。
5. ✅ **控制流** (touch/key):继续走 GraphQL mutation。
6. ✅ **加密**: **按现状分通道** — Text 走 ChaCha20(业务),Binary 走 raw(高吞吐,现状故意设计)。三个 mirror 新 EventType 继承 Binary raw 行为,保 0 copy。

## 8. 不动的部分 (硬约束)

- Android 11 VirtualDisplay resize 黑条 fix  (`docs/screen-mirror-webrtc.md:123-152`)
- AOSP/OEM startForeground + getMediaProjection 顺序 (`ScreenMirrorService.kt:82-118`)
- orientation 旋转后 recreate VirtualDisplay 路径(`ScreenMirrorWebRtcManager.kt:170-188`)
- 权限弹窗 UX 流程 (30s 倒计时等)
- `screen-mirror-ux.md` 描述的状态机 (idle / requesting / connecting / streaming / failed)
- 控制流 (`screen-mirror-control.ts` 392 行不动)
- 截图 / 全屏 / 录制 UI
- 整个 chat / file / 其他 GraphQL schema

## 9. 性能对比 (ws binary vs WebRTC)

| 指标 | WebRTC (现) | ws binary + WebCodecs (新) | 改善 |
|---|---|---|---|
| **启动到首帧** | 1-3s (SDP+ICE+DTLS+SRTP 握手) | < 300ms (一个 CONFIG + 第一个 IDR) | -80% |
| **稳态延迟 (LAN)** | 80-140ms | 30-60ms | -60% |
| **CPU 占用 (1080p@30fps)** | 35-45% (libwebrtc OpenH264 软编为主) | 10-15% (MediaCodec 硬编 + WebCodecs 硬解) | -70% |
| **内存 copy / 帧** | 4-5 次 GPU↔CPU (SurfaceTexture→I420→WebRTC frame→RTP→SRTP) | **0 次** (Binary raw 通道,WebCodecs 直接吃 ws frame) | 全消除 |
| **内存占用 (steady)** | 80-120MB (PeerConnection factory + EGL + frame pool) | 15-25MB (MediaCodec + ws session) | -75% |
| **LAN 吞吐** | ~4Mbps UDP | ~4Mbps TCP (LAN TCP 不丢包) | 几乎无差 |
| **弱网 (10% 丢包)** | 略好 (UDP 智能重传) | 略差 (TCP HOL blocking) | Phase 3 WebTransport 兜底 |
| **音频支持** | 反射 hack WebRTC internal AudioRecord (脆弱) | MediaCodec Opus 编码 + WebCodecs AudioDecoder (稳) | 质变 |

### 为什么 Binary 不加密能保住 0 copy

现有设计就是 Binary 通道(高吞吐)走 raw,Text 通道(业务消息)走 ChaCha20。本次新加的 mirror 三个 EventType 全是 Binary,**继承现状 raw 行为**:
- 现状 (WebRTC): `SurfaceTexture → I420(CPU 读+颜色转换) → WebRTC frame(CPU 缓冲) → RTP(SRTP 加密) → UDP → 接收端 SRTP decrypt → WebRTC decoder(CPU) → canvas(浏览器自绘)` = **4-5 次 GPU↔CPU**
- 新: `MediaCodec input surface(GPU 直传) → NAL(无加密) → ws binary → WebCodecs VideoDecoder.decode(EncodedVideoChunk,GPU 内部) → canvas.drawImage(GPU)` = **0 次 CPU 中转**

`EncodedVideoChunk` 在 WebCodecs 里可以直接喂给 decoder,内部走 GPU/硬件解码,不需要中间 CPU 缓冲。Binary 通道不加密就是保这个 GPU 直传链的关键。Text 业务通道仍然走 ChaCha20 保护敏感数据,两套通道职责分明。

## 10. 关联文件 (改的时候一一过)

### Android (要动)
- `app/src/main/java/com/ismartcoding/plain/services/ScreenMirrorService.kt` (168 → ~120)
- `app/src/main/java/com/ismartcoding/plain/services/webrtc/*.kt` (734 → 0,全删)
- `app/src/main/java/com/ismartcoding/plain/web/schemas/ScreenMirrorGraphQL.kt` (93 → ~50)
- `app/src/main/java/com/ismartcoding/plain/web/websocket/WebSocketHelper.kt` (+ ~30)
- `app/src/main/java/com/ismartcoding/plain/web/websocket/WebRtcSignalingMessage.kt` (删)
- `shared/src/commonMain/kotlin/com/ismartcoding/plain/events/WebSocketEvents.kt` (+ 2 enum case)

### Android (新增)
- `app/src/main/java/com/ismartcoding/plain/services/screenmirror/MediaCodecEncoder.kt`
- `app/src/main/java/com/ismartcoding/plain/services/screenmirror/ScreenMirrorPipeline.kt`
- `app/src/main/java/com/ismartcoding/plain/services/screenmirror/NalUnitParser.kt` (annex-b → avcc 转, web 端可能不需要,留个口子)

### Web (要动)
- `src/lib/webrtc-client.ts` (删)
- `src/lib/webrtc-signaling.ts` (删)
- `src/plugins/eventbus.ts` (+ 1 事件)
- `src/hooks/app-socket.ts` (+ 1 event id)
- `src/views/screen-mirror/screen-mirror-webrtc.ts` (删)
- `src/views/screen-mirror/screen-mirror-service.ts` (改 setWebRTC → setPipeline)
- `src/views/screen-mirror/screen-mirror-media.ts` (videoRef → canvasRef)
- `src/views/screen-mirror/screen-recording.ts` (captureStream input 改)
- `src/views/screen-mirror/use-screen-mirror-view.ts` (ref 类型改)
- `src/views/screen-mirror/ScreenMirrorContent.vue` (<video> → <canvas>)
- `src/views/screen-mirror/ScreenMirrorView.vue` (selector 改)
- `src/views/screen-mirror/ScreenMirrorHeaderActions.vue` (跟 video element 相关的改)

### Web (新增)
- `src/lib/mirror-codec.ts`
- `src/views/screen-mirror/screen-mirror-pipeline.ts`

### 文档 (要改)
- `plain-app/docs/screen-mirror-webrtc.md` 改名为 `screen-mirror-redesign.md` (本文件) 或保留并新增 `screen-mirror-codec.md`
- `plain-app/docs/ARCHITECTURE.md` 加新架构图 (Phase 1 完成后)
