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
