# Screen Mirror (WebRTC) – Implementation Notes

This document describes how Screen Mirror works in this project after migrating from sending JPEG frames to WebRTC video.

## Goals

- Low-latency screen mirroring that feels “real-time” on LAN.
- Smooth delivery (avoid bursty frame updates and multi-second backlog).
- Keep signaling on the app’s existing messaging channel (reliable delivery, encrypted transport), but keep the media path on WebRTC.
- Clear separation of responsibilities:
  - Android owns capture + encoding + WebRTC sending.
  - The Android Service is only lifecycle/permission glue.
  - The web UI is a WebRTC answerer that renders a video element.

## Architecture Overview

### Android (producer)

- Captures the device screen via MediaProjection.
- Renders the screen into a VirtualDisplay surface.
- Feeds frames into WebRTC as a screen-cast video track.
- Acts as the offerer in SDP negotiation.

Audio (optional):

- Produces an audio track.
- On supported Android versions, system audio can be captured via playback-capture (requires the relevant permission).

### Web (consumer)

- Creates an RTCPeerConnection.
- Acts as the answerer.
- Attaches the received remote stream to a video element (autoplay + playsinline + muted).

Low-latency playback:

- The receiver side minimizes buffering on supported browsers by reducing the jitter buffer target. This reduces the “always-behind” feeling on stable networks.

### Signaling transport

- Signaling messages are small JSON objects (ready / offer / answer / ICE candidate).
- They are carried over the project’s existing secure messaging channel (implementation detail may vary by platform), separate from the WebRTC media.

## Signaling Flow

### Roles

- Android = Offerer
- Web = Answerer

### Message types (conceptual)

- ready
- offer (SDP)
- answer (SDP)
- ice_candidate

### Handshake sequence

1. Web initializes RTCPeerConnection.
2. Web sends ready.
3. Android creates (or refreshes) a peer connection, creates an offer, and sends offer.
4. Web sets the remote offer, creates an answer, and sends answer.
5. Both sides exchange ICE candidates.
6. Web receives the remote track(s) and starts playback.

The ready message prevents a deadlock where the web UI is waiting for an offer but Android does not start negotiation yet.

## Video Capture & Latency Characteristics (Android)

### Capture pipeline

- Capture starts once MediaProjection permission is granted.
- A VirtualDisplay is created once and resized on orientation/quality changes (avoids re-creating MediaProjection).
- Frames are forwarded into WebRTC as a screen-cast source.

### Frame-rate and backlog control

- Capture is capped to 30 fps.
- Frame dropping is used intentionally when necessary to prevent encoder overload and latency build-up (it is better to drop frames than to deliver them late).

## Quality Modes

The UI exposes three modes:

- AUTO
  - Targets high quality when the network is good.
  - Uses periodic WebRTC stats to adapt resolution (typically between 1080p and 720p) based on bitrate, loss, and RTT.
- HD
  - Prefers sharper output (1080p-class capture) and higher bitrate.
- SMOOTH
  - Prioritizes “feels real-time” latency.
  - Uses 720p-class capture and parameters that favor stable frame-rate and fast bitrate convergence.

When the quality mode changes:

- Android updates capture size and encoder bitrate.
- The web side may restart negotiation to make behavior deterministic across browsers.

## Troubleshooting

### Connected but no video

- Ensure the video element can autoplay:
  - It must be muted.
  - It must exist in the DOM when tracks arrive (or the stream must be attached once it exists).
- Verify signaling order:
  - Web sent ready.
  - Android sent offer.
  - Web sent answer.
- Check Android logs for capture start and VirtualDisplay creation/resizing.

### Quality changed and stream looks stuck / blank

- Confirm the quality change request reached Android.
- Confirm Android resized capture and updated bitrate.
- Confirm the web side restarted the WebRTC session (ready → offer → answer) if that is part of the UI flow.

## Known Device Edge Cases (Must Keep)

These cases were observed on real Android 11 devices. They are easy to regress if capture-size logic is simplified.

### Case A: using displayMetrics causes wrong aspect ratio on Android 11

- Symptom:
  - Mirrored image can have black bars / wrong crop on some devices.
- Root cause:
  - `displayMetrics.widthPixels/heightPixels` may exclude navigation-bar area on Android <= 11.
  - The produced capture surface size does not match real physical display pixels.
- Required behavior:
  - Use real display size for capture, not app-window metrics.

### Case B: Switching quality modes causes black bars on some Android 11 devices

- Symptom:
  - First connection in HD/AUTO mode displays correctly (no black bars).
  - After switching to SMOOTH and then back to HD, right and bottom edges show black bars.
  - The logged capture dimensions are identical both times — the numbers are correct, but the rendering is wrong.
- Root cause:
  - `VirtualDisplay.resize()` on some Android 11 devices does not correctly update the internal rendering region when going from a smaller size back to a larger one (e.g. 720p → 1080p). The compositor continues to render at the old smaller area within the now-larger Surface.
  - Initial creation with `createVirtualDisplay()` always works correctly.
  - Previous attempts to fix via scale-factor capping or DPI scaling did not help because the bug is in the resize path itself, not in the computed dimensions.
- Fix:
  - **Never use `VirtualDisplay.resize()` on Android ≤ 15**. Instead, release the old VirtualDisplay and recreate it from the saved MediaProjection each time the quality or orientation changes.
  - On Android 16+ (API 36), `createVirtualDisplay` may only be called once per `MediaProjection` instance; use `VirtualDisplay.resize()` on those versions.
  - Also recreate the Surface wrapper around SurfaceTexture to ensure clean state after buffer size change (Android ≤ 15 path only).
- Required behavior:
  - `resizeVirtualDisplay()` must release the old VirtualDisplay and create a new one — not call `vd.resize()`.
  - The MediaProjection must be kept alive (it is obtained once) and reused for new VirtualDisplay instances.
  - For Android <= 11, prefer `Display.Mode.physicalWidth/physicalHeight` (with rotation handling) as capture base size.
  - Keep a fallback to `getRealSize()` only if mode dimensions are invalid.

## Regression Checklist (Screen Size / Black Bars)

When changing `ScreenMirrorWebRtcManager` capture logic, verify all items below:

- **Android 16+ (API 36) uses `VirtualDisplay.resize()`**: `MediaProjection#createVirtualDisplay` may only be called once on Android 16+. On those versions `resizeVirtualDisplay()` calls `resize()` instead of recreating.
- **Android ≤ 15 uses recreate, never `VirtualDisplay.resize()`**: The `resize()` method is broken on some Android 11 devices when upscaling. The VirtualDisplay is released and a new one created from the saved MediaProjection.
- Android 11 physical device:
  - HD mode has no right/bottom black bars.
  - Smooth mode remains correct.
  - **Switch SMOOTH → HD must not produce black bars** (the critical regression case).
- Rotate portrait/landscape after mirroring starts:
  - No black bars after VirtualDisplay recreation.
- Check logs:
  - `VirtualDisplay created {w}x{h} dpi={dpi}` (initial) and `VirtualDisplay recreated {w}x{h} dpi={dpi}` (on quality/orientation change) values are consistent.
- Android version matrix:
  - Android 12+ path still uses `WindowMetrics`.
  - Android <= 11 path still uses display mode physical size first.
- If refactoring metrics code:
  - Do not replace real-screen logic with `displayMetrics.widthPixels/heightPixels`.
  - Do not reintroduce `VirtualDisplay.resize()` as an "optimization".

## Suggested Quick Manual Test Script

1. Connect web mirror, set HD, observe full-bleed frame for 20s.
2. Switch to Smooth, observe no crop/black bar regression.
3. **Switch back to HD — verify NO black bars on right/bottom** (critical case).
4. Rotate device twice, verify frame remains full-bleed in both modes.
5. Switch AUTO ↔ HD ↔ SMOOTH repeatedly, verify no persistent edge bars.
3. Rotate device twice, verify frame remains full-bleed in both modes.
4. Switch AUTO ↔ HD ↔ Smooth repeatedly, verify no persistent edge bars.
