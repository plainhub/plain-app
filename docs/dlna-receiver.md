# DLNA Receiver

## Overview

The DLNA Receiver feature turns the PlainApp Android device into a **UPnP Digital Media Renderer (DMR)**. Any DLNA-compatible controller — such as a TV remote app, VLC, Windows Media Player, or a smart TV — can discover PlainApp on the local network and push media URLs to it for playback.

This is the **receive** side of DLNA. PlainApp already has a DLNA **send** side (CastViewModel / UPnPController) that casts local files to external TVs. The receiver feature adds the opposite direction.

---

## Architecture

```
                      ┌────────────────────────────────────────────────────────┐
                      │                  Local Wi-Fi Network                   │
                      │                                                        │
  ┌──────────────────┐│  SSDP NOTIFY / M-SEARCH          HTTP SOAP POST       │
  │  DLNA Controller ││◄──────────────────────────────►  /AVTransport/control │
  │  (TV / PC / App) ││                                                        │
  └──────────────────┘│                                                        │
                      └────────────────────────────────────────────────────────┘
                                                       │ DlnaCommand (Channel)
                                                       ▼
  ┌──────────────────────────────────────────────────────────────────────────┐
  │                          Android App (PlainApp)                          │
  │                                                                          │
  │  ┌─────────────────────────────────────────────────────────────────┐    │
  │  │                     DlnaRenderer (Coordinator)                  │    │
  │  │  CoroutineScope (SupervisorJob)                                 │    │
  │  │   ├── DlnaHttpServer coroutine  (port 7878, Java ServerSocket)  │    │
  │  │   └── DlnaSsdpAdvertiser coroutine  (UDP 239.255.255.250:1900)  │    │
  │  └─────────────────────────────────────────────────────────────────┘    │
  │                          │                                               │
  │                          │  DlnaRendererState (StateFlows + Channel)     │
  │                          │                                               │
  │  ┌────────────────────────────────────────────────────────────────────┐ │
  │  │              DlnaReceiverViewModel                                 │ │
  │  │  • Processes DlnaCommand channel                                   │ │
  │  │  • Updates DlnaRendererState.playbackState                         │ │
  │  │  • Syncs ExoPlayer position → DlnaRendererState                    │ │
  │  └────────────────────────────────────────────────────────────────────┘ │
  │                          │                                               │
  │  ┌────────────────────────────────────────────────────────────────────┐ │
  │  │              DlnaReceiverPage (Compose UI)                         │ │
  │  │  • Toggle switch to enable/disable receiver                         │ │
  │  │  • Device info card (name, IP, port)                                │ │
  │  │  • DlnaReceiverPlayerSection (ExoPlayer + controls)                │ │
  │  └────────────────────────────────────────────────────────────────────┘ │
  └──────────────────────────────────────────────────────────────────────────┘
```

---

## File Structure

```
app/src/main/java/com/ismartcoding/plain/
├── features/dlna/
│   ├── DlnaCommand.kt          Sealed class: SetUri | Play | Pause | Stop | Seek
│   ├── DlnaRendererState.kt    Singleton StateFlows + Channel<DlnaCommand>
│   ├── DlnaXmlTemplates.kt     Device description XML and SCPD XML strings
│   ├── DlnaSoapHandler.kt      SOAP request parser + SOAP response builder
│   ├── DlnaHttpServer.kt       Coroutine HTTP server (java.net.ServerSocket)
│   ├── DlnaSsdpAdvertiser.kt   SSDP NOTIFY/M-SEARCH via MulticastSocket
│   └── DlnaRenderer.kt         Coordinator — starts/stops HTTP + SSDP servers
│
├── ui/models/
│   └── DlnaReceiverViewModel.kt  ViewModel bridging commands → ExoPlayer
│
└── ui/page/dlna/
    ├── DlnaReceiverPage.kt        Main screen (toggle, info card, player)
    └── DlnaReceiverPlayerSection.kt  ExoPlayer + seek/play controls
```

---

## UPnP / DLNA Protocol Implementation

### SSDP (Simple Service Discovery Protocol)
- Multicast group `239.255.255.250:1900` via `MulticastSocket` (reuses same socket as `UPnPDiscovery` with `SO_REUSEADDR`).
- On start: sends three `NOTIFY ssdp:alive` datagrams for root device, MediaRenderer device type, and AVTransport service type.
- While running: listens for incoming `M-SEARCH` requests and replies with `HTTP/1.1 200 OK` search responses.
- Periodic re-announce every 30 s (within `CACHE-CONTROL: max-age=1800`).
- On stop: sends `NOTIFY ssdp:byebye` datagrams for graceful removal.

### HTTP Server (port 7878)
Implemented with a plain `java.net.ServerSocket` inside a coroutine. No new library dependency.

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/description.xml` | UPnP device description (MediaRenderer:1) |
| GET | `/AVTransport/scpd.xml` | Service capability document |
| GET | `/RenderingControl/scpd.xml` | RenderingControl SCPD (empty) |
| POST | `/AVTransport/control` | AVTransport SOAP actions |
| POST | `/RenderingControl/control` | RenderingControl (volume — stub) |
| SUBSCRIBE | `/AVTransport/event` | Event subscription — acknowledged, not fully implemented |
| UNSUBSCRIBE | `/AVTransport/event` | Event unsubscription |

### Supported SOAP Actions

| Action | Behavior |
|--------|----------|
| `SetAVTransportURI` | Extracts `CurrentURI` and DIDL-Lite title, sends `DlnaCommand.SetUri` |
| `Play` | Sends `DlnaCommand.Play` |
| `Pause` | Sends `DlnaCommand.Pause` |
| `Stop` | Sends `DlnaCommand.Stop` |
| `Seek` | Parses `Target` (HH:MM:SS), sends `DlnaCommand.Seek` |
| `GetTransportInfo` | Returns current `DlnaPlaybackState` |
| `GetPositionInfo` | Returns position/duration from `DlnaRendererState` |
| `GetMediaInfo` | Returns stub response |
| `GetDeviceCapabilities` | Returns `NETWORK` play media |
| `SetPlayMode` | Returns success (normal mode only) |

---

## State Machine

```
NO_MEDIA_PRESENT  ──SetAVTransportURI──►  TRANSITIONING
                                                │
                                         (media prepared)
                                                │
    STOPPED  ◄──Stop──  PLAYING  ◄──Play──  STOPPED
       │                   │
       │                 Pause
       │                   │
       └────────────►  PAUSED ────►  PLAYING
```

`DlnaRendererState.playbackState` drives the ExoPlayer in `DlnaReceiverPlayerSection` via `LaunchedEffect(playbackState)`.

---

## Command Flow

```
DLNA Controller                DlnaHttpServer           DlnaRendererState          DlnaReceiverViewModel          ExoPlayer
      │                              │                         │                            │                        │
      │─── POST /AVTransport/control ►│                         │                            │                        │
      │    SOAPAction: SetAVTransportURI                        │                            │                        │
      │◄── 200 OK ──────────────────-│                         │                            │                        │
      │                              │──trySend(SetUri)────►   │                            │                        │
      │                              │                         │──mediaUri.value = uri──►   │                        │
      │                              │                         │                            │─── setMediaItem ──────►│
      │                              │                         │                            │─── prepare ───────────►│
      │─── POST /AVTransport/control ►│                         │                            │                        │
      │    SOAPAction: Play                                     │                            │                        │
      │◄── 200 OK ──────────────────-│                         │                            │                        │
      │                              │──trySend(Play)──────►   │                            │                        │
      │                              │                         │──playbackState=PLAYING──►  │                        │
      │                              │                         │                            │─── player.play ───────►│
```

---

## Limitations (MVP)

- **Event subscription**: `SUBSCRIBE` is acknowledged with a static SID but no events are pushed to the controller. Controllers that depend on push events for position tracking will poll `GetPositionInfo` instead.
- **RenderingControl**: Volume control SOAP actions are accepted but ignored (device volume is not modified).
- **Multiple instances**: Only one renderer is supported at a time (single `DlnaRenderer` singleton).
- **Port conflicts**: If port 7878 is in use, the HTTP server silently fails. Future work: auto-select next available port.
- **Subtitle/DRM**: No support for subtitle tracks or DRM-protected media.

---

## Permissions Used

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | HTTP server socket |
| `ACCESS_WIFI_STATE` | Read local IP address |
| `CHANGE_WIFI_MULTICAST_STATE` | Acquire `MulticastLock` for SSDP |
| `WAKE_LOCK` | (inherited) Keep CPU awake during playback |

All permissions are already declared in `AndroidManifest.xml`.

---

## Future Work

- Push UPnP events to subscribed controllers (position/state changes).
- Support Chromecast-style `SetNextAVTransportURI` for gapless queue.
- Show artwork from DIDL-Lite metadata.
- Auto-start receiver on boot (foreground service option).
- i18n: Translate UI strings to all supported locales once the feature is stable.
