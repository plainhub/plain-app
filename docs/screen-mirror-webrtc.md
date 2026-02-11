# Screen Mirror (WebRTC) – Requirements & Implementation Notes

This document describes how Screen Mirror works in this project after migrating from WebSocket JPEG frame streaming to WebRTC.

## Goals

- High-performance screen mirroring (low latency, smoother frame delivery).
- Use WebRTC video instead of sending images over WebSocket.
- Keep signaling over the project’s existing encrypted WebSocket protocol (ChaCha20, binary frames with an integer event type prefix).
- Make responsibilities clear:
  - Android WebRTC/capture logic lives in a dedicated manager.
  - The Android Service is a thin lifecycle/permission wrapper.
  - The web UI is a WebRTC *answerer* that renders a `<video>`.

## Architecture Overview

### Android (producer)

- Captures the device screen via `MediaProjection`.
- Feeds frames into WebRTC using `org.webrtc.ScreenCapturerAndroid`.
- Creates a `VideoTrack` and adds it to a `PeerConnection`.
- Acts as the **offerer** in SDP negotiation.

Primary responsibility class:

- `app/src/main/java/com/ismartcoding/plain/services/webrtc/ScreenMirrorWebRtcManager.kt`

The service wrapper:

- `app/src/main/java/com/ismartcoding/plain/services/ScreenMirrorService.kt`

### Web (consumer)

- Creates an `RTCPeerConnection`.
- Acts as the **answerer**.
- Attaches the received remote stream to a `<video>` element (autoplay + muted).

Primary files:

- `plain-web/src/lib/webrtc-client.ts`
- `plain-web/src/lib/webrtc-signaling.ts`
- `plain-web/src/views/ScreenMirrorView.vue`

### Signaling Transport (encrypted WebSocket)

- Signaling messages are JSON objects, but transported over the existing **encrypted** WebSocket channel.
- The encrypted message payload is carried inside a binary frame prefixed by an integer event type.
- This project reserves an event type for WebRTC signaling:
  - Android enum: `WEBRTC_SIGNALING`

On the server side, WebSocket routing and encryption integration is handled in the existing WebSocket module (Ktor).

## Signaling Flow

### Roles

- Android = Offerer
- Web = Answerer

### Message types

Typical signaling message shapes (conceptual):

- `{"type":"ready"}`
- `{"type":"offer","sdp":"..."}`
- `{"type":"answer","sdp":"..."}`
- `{"type":"ice_candidate","candidate":{...}}`

### Handshake sequence

1. Web connects to the app’s WebSocket and completes the existing encrypted handshake.
2. Web initializes `RTCPeerConnection`.
3. Web sends `ready` to Android (encrypted signaling event).
4. Android receives `ready`, creates a fresh `PeerConnection` (if needed), creates an SDP offer, and sends `offer`.
5. Web receives `offer`, sets remote description, creates `answer`, and sends `answer`.
6. Both sides exchange ICE candidates.
7. Web receives the remote track and starts playback.

The `ready` message exists to avoid a negotiation deadlock where the web UI is waiting for an offer but Android never starts negotiation.

## Video Capture / Streaming Details (Android)

- Screen capture starts via `ScreenCapturerAndroid.startCapture(width, height, fps)`.
- Quality changes use `ScreenCapturerAndroid.changeCaptureFormat(width, height, fps)` when possible.

Logging:

- The WebRTC manager logs requested capture resolutions and whether `startCapture()` / `changeCaptureFormat()` succeeds or throws.
- Use these logs to confirm that capture is running at the expected resolution after a quality change.

## Quality Presets

The web UI exposes exactly three presets:

- 480p
- 720p
- 1080p

Selecting a preset:

- Sends a GraphQL mutation to update the quality/resolution on Android.
- Restarts WebRTC negotiation from the web side by re-initializing the WebRTC client and sending `ready` again.

Notes:

- Resolution changes can require renegotiation; restarting negotiation after updating the preset makes the behavior deterministic.

## Troubleshooting

### ICE connected but no video displayed

Check these first:

- Web `<video>` element:
  - Must exist in DOM when track arrives.
  - Must be `muted` to allow autoplay in most browsers.
- Verify signaling:
  - Web sends `ready`.
  - Android sends `offer`.
  - Web sends `answer`.
- Verify capture logs on Android:
  - Confirm `startCapture()` succeeded.
  - Confirm `changeCaptureFormat()` succeeded on quality change.

### Quality changed, stream is blank

- Confirm the GraphQL mutation succeeded.
- Confirm Android logs show the new capture format.
- Confirm the web side restarted negotiation (new `ready` -> `offer` -> `answer`).

## Key “where to look” map

- Android stream production (capture/track/SDP/ICE):
  - `ScreenMirrorWebRtcManager.kt`
- Android lifecycle + permissions + delegation:
  - `ScreenMirrorService.kt`
- Web negotiation + video attachment:
  - `ScreenMirrorView.vue`, `webrtc-client.ts`
- Encrypted signaling transport:
  - `webrtc-signaling.ts` (web) and WebSocket helper (android/server)
