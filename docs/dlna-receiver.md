# DLNA Receiver

## Overview

The DLNA Receiver feature turns PlainApp into a **UPnP Digital Media Renderer (DMR)**. Any DLNA-compatible controller — VLC, a TV remote app, Bubble UPnP, Windows Media Player — can discover PlainApp on the local network and push audio, image, or video URLs to it for playback.

PlainApp also has a DLNA **sender** side (`CastViewModel` / `DlnaTransportController`) that casts local files to external TVs. This document covers both sides.

---

## Architecture

```
  ┌──────────────────────────────────────────────────────────┐
  │                        Local Wi-Fi                       │
  │                                                          │
  │   SSDP multicast 239.255.255.250:1900  ◄──────────────► │
  │   HTTP SOAP POST /AVTransport/control  ◄──────────────► │
  └──────────────────────────────────────────────────────────┘
              ▲ RECEIVER (incoming cast)        ▲ SENDER (outgoing cast)
              │                                 │
  ┌───────────┴──────────────┐    ┌─────────────┴──────────────┐
  │  DlnaRenderer            │    │  DlnaTransportController   │
  │  DlnaHttpServer          │    │  DlnaDeviceScanner (SSDP)  │
  │  DlnaSsdpAdvertiser      │    │  DlnaEventSubscriber       │
  │  DlnaSoapHandler         │    │  CastPlayer / CastViewModel│
  │  DlnaReceiverViewModel   │    └────────────────────────────┘
  │  DlnaReceiverPage        │
  └──────────────────────────┘
```

---

## File Structure

```
app/src/main/java/com/ismartcoding/plain/
├── features/dlna/
│   ├── DlnaMediaType.kt              Enum: VIDEO | AUDIO | IMAGE | UNKNOWN
│   ├── DlnaCommand.kt                Sealed class: SetUri | Play | Pause | Stop | Seek
│   ├── DlnaRendererState.kt          Singleton StateFlows + Channel<DlnaCommand>
│   ├── PendingCastRequest.kt         Data class for incoming cast requests (senderIp, senderName, mediaUri, mediaTitle, mediaType, albumArtUri)
│   ├── DlnaXmlTemplates.kt           Device description XML and SCPD XML strings
│   ├── receiver/
│   │   ├── DlnaRenderer.kt           Coordinator — starts/stops HTTP + SSDP coroutines
│   │   ├── DlnaHttpServer.kt         Coroutine HTTP server (java.net.ServerSocket, port 7878)
│   │   ├── DlnaHttpServerSupport.kt  readHttpLine / readBodyBytes / httpOk / parseDlnaTimeToMs helpers
│   │   ├── DlnaSoapHandler.kt        SOAP parser + response builder + DIDL-Lite extractors
│   │   └── DlnaSsdpAdvertiser.kt     SSDP NOTIFY/M-SEARCH via MulticastSocket
│   ├── sender/
│   │   ├── DlnaTransportController.kt  SOAP client — SetAVTransportURI / Play / Pause / Stop / Seek
│   │   ├── DlnaDeviceScanner.kt        SSDP M-SEARCH to discover renderers on LAN
│   │   └── DlnaEventSubscriber.kt      SUBSCRIBE / RENEW / UNSUBSCRIBE for rendering events
│   └── common/
│       ├── DlnaSoap.kt                 SOAP envelope constants and builders (shared by sender + receiver)
│       └── DlnaDevice.kt               UPnP device model
│
├── features/media/CastPlayer.kt       Sender-side playback queue + current URI state
│
├── ui/models/
│   ├── DlnaReceiverViewModel.kt       Processes DlnaCommand channel → DlnaRendererState + ExoPlayer
│   └── CastViewModel.kt + CastViewModelCast.kt  Sender-side ViewModel
│
└── ui/page/dlna/
    ├── DlnaReceiverPage.kt             Main receiver screen (toggle, info, cast approval)
    ├── DlnaReceiverAudioPlayer.kt      Full-screen audio player (gradient, album art, controls)
    ├── DlnaReceiverImageViewer.kt      Full-screen image viewer (Coil AsyncImage)
    ├── DlnaReceiverVideoPlayer.kt      Full-screen video player (ExoPlayer)
    └── DlnaAudioPlayerControls.kt      Extracted seek bar + play/pause button
```

---

## Receiver: How It Works

### 1. SSDP Discovery (DlnaSsdpAdvertiser)

- Joins multicast group `239.255.255.250:1900` via `MulticastSocket` (`SO_REUSEADDR`).
- On start: broadcasts 3 `NOTIFY ssdp:alive` datagrams: root device, `MediaRenderer:1` device type, `AVTransport:1` service type.
- Re-announces every 30 s (`CACHE-CONTROL: max-age=1800`).
- Listens for `M-SEARCH` requests and replies with `HTTP/1.1 200 OK` unicast responses containing the HTTP server's `LOCATION` URL.
- On stop: sends `NOTIFY ssdp:byebye` datagrams.

### 2. HTTP Server (DlnaHttpServer, port 7878)

`java.net.ServerSocket` inside a coroutine. Each client connection gets its own `launch {}`.

> **Important**: The HTTP body is read at the **byte** level using `readBodyBytes(BufferedInputStream, contentLength)`, not via `BufferedReader` + `CharArray`. `Content-Length` is a byte count. Reading by char count fails for multi-byte UTF-8 (e.g. Chinese titles — 1 Chinese char = 3 bytes) and causes a socket timeout, silently breaking `SetAVTransportURI`.

| Method | Path | Handler |
|--------|------|---------|
| `GET` | `/description.xml` | `DlnaXmlTemplates.deviceDescription()` |
| `GET` | `*scpd.xml` | `DlnaXmlTemplates.scpdXml` |
| `POST` | `*control` / `*AVTransport` | `handleSoap()` |
| `POST` | `*RenderingControl` | Stub volume response |
| `SUBSCRIBE` | any | Static SID acknowledgment |
| `UNSUBSCRIBE` | any | 200 OK |

### 3. SOAP Parsing (DlnaSoapHandler)

`parseSoapAction(header, body)` extracts the action name and a `Map<String, String>` of parameters from the XML body using `XmlPullParser`.

**`SetAVTransportURI` flow:**
1. Extract `CurrentURI` and `CurrentURIMetaData` from params.
2. `extractTitleFromDidlMeta(meta)` — parses `<dc:title>` from DIDL-Lite XML, HTML-unescapes entities.
3. `cleanMediaTitle(raw)` — URL-decodes percent-encoding, strips media file extensions.
4. `extractMediaTypeFromDidlMeta(meta, uri)` — reads `<upnp:class>` (e.g. `object.item.audioItem.musicTrack` → `AUDIO`); falls back to URI file extension.
5. `extractAlbumArtUriFromDidlMeta(meta)` — reads `<upnp:albumArtURI>` (handles attributes like `dlna:profileID`).
6. Writes `PendingCastRequest` → `DlnaRendererState.rawPendingCastRequest`.

### 4. Cast Request Rules (DlnaReceiverViewModel.startRuleCheck)

Collects `rawPendingCastRequest`, checks against `DlnaAllowedSendersPreference` / `DlnaDeniedSendersPreference`:

- **In allowed list** → auto-accept: sends `DlnaCommand.SetUri` immediately.
- **In denied list** → silently discard.
- **Unknown** → copies to `pendingCastRequest` for user to approve/reject in the UI dialog.

### 5. Command Processing (DlnaReceiverViewModel.startCommandProcessing)

Processes `DlnaRendererState.commandChannel`:

| Command | Effect |
|---------|--------|
| `SetUri` | Sets `mediaUri`, `mediaTitle`, `mediaAlbumArtUri`, `mediaType`, `playbackState = TRANSITIONING` |
| `Play` | `playbackState = PLAYING` |
| `Pause` | `playbackState = PAUSED` |
| `Stop` | Seeks to 0, `playbackState = STOPPED` |
| `Seek` | Sets `seekTargetMs` |

### 6. UI Routing (DlnaReceiverPage)

When `mediaUri` is non-empty and `playbackState != NO_MEDIA_PRESENT`, routes by `mediaType`:

| `DlnaMediaType` | Composable |
|-----------------|------------|
| `AUDIO` | `DlnaReceiverAudioPlayer` — deep blue gradient, album art (Coil `AsyncImage`, falls back to music icon on error), seek bar, play/pause |
| `IMAGE` | `DlnaReceiverImageViewer` — full-screen Coil `AsyncImage`, loading spinner |
| `VIDEO` / `UNKNOWN` | `DlnaReceiverVideoPlayer` — ExoPlayer, full-screen |

---

## Sender: How It Works

### 1. Discovery (DlnaDeviceScanner)

Sends SSDP `M-SEARCH` datagrams targeting `urn:schemas-upnp-org:service:AVTransport:1`. Responses include a `LOCATION` URL. The scanner fetches each `LOCATION`, parses the device XML, checks for `AVTransport` service, and emits `DlnaDevice` objects.

### 2. Casting (CastViewModelCast)

```
castItem(item: IMedia)
  → UrlHelper.getMediaHttpUrl(item.path)      // registers path in mediaPathMap, returns http://<ip>:<port>/media/<id>.<ext>
  → UrlHelper.getAlbumArtHttpUrl(albumUri)    // for DAudio: maps content:// album art URI → http URL (served by /media/{id} route)
  → DlnaTransportController.setAVTransportURIAsync(device, mediaUrl, item.title, albumArtUri)
  → DlnaTransportController.playAVTransportAsync(device)
```

### 3. DIDL-Lite Metadata (DlnaTransportController.buildDidlLiteMetadata)

When `title` is provided, the sender builds a proper DIDL-Lite XML and sets it as `CurrentURIMetaData`. This is what the receiver uses to get the title, media type, and album art.

```xml
<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/"
           xmlns:dc="http://purl.org/dc/elements/1.1/"
           xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
  <item id="0" parentID="-1" restricted="0">
    <dc:title>Song Title</dc:title>
    <upnp:class>object.item.audioItem.musicTrack</upnp:class>
    <upnp:albumArtURI>http://192.168.1.x:8080/media/art_xxx.jpg</upnp:albumArtURI>
  </item>
</DIDL-Lite>
```

The DIDL-Lite XML is then XML-escaped (all `<`, `>`, `&` replaced) before embedding in the SOAP body.

`upnp:class` is inferred from the URL file extension:
- `.mp3 / .m4a / .flac / .ogg / .aac / .wav / .opus / .wma` → `object.item.audioItem.musicTrack`
- `.jpg / .jpeg / .png / .gif / .webp / .bmp` → `object.item.imageItem`
- everything else → `object.item.videoItem`

### 4. Album Art HTTP Serving

`UrlHelper.getAlbumArtHttpUrl(albumUri: Uri)` maps `content://media/external/audio/albumart/<id>` → `http://<ip>:<port>/media/art_<ts>.jpg`. The existing `/media/{id}` HTTP route in `HttpModule` handles `content://` URIs via `contentResolver.openInputStream()`.

### 5. Event Subscription (DlnaEventSubscriber)

After casting, `trySubscribeEvent()` sends a `SUBSCRIBE` request to the renderer. If it returns a non-empty `SID`:
- Position updates come via callbacks → `CastPlayer.updatePositionInfo()`.
- A `startPositionUpdater()` job also polls `GetPositionInfo` every second as a fallback.

---

## State Machine (Receiver)

```
NO_MEDIA_PRESENT  ──SetAVTransportURI──►  TRANSITIONING
                                                │
                                         (ExoPlayer prepared)
                                                │
    STOPPED  ◄──Stop──  PLAYING  ◄──Play──  STOPPED
       │                   │
       │                 Pause
       │                   │
       └────────────►  PAUSED ────►  PLAYING
```

---

## Full Command Flow (Receiver)

```
DLNA Controller            DlnaHttpServer        rawPendingCastRequest   DlnaReceiverViewModel   DlnaRendererState   UI
      │                          │                        │                       │                     │              │
      │── POST SetAVTransportURI►│                        │                       │                     │              │
      │◄─ 200 OK ────────────────│                        │                       │                     │              │
      │                          │── PendingCastRequest──►│                       │                     │              │
      │                          │                        │── (rule check) ──────►│                     │              │
      │                          │                        │                       │── SetUri ──────────►│              │
      │                          │                        │                       │   mediaUri           │              │
      │                          │                        │                       │   mediaTitle         │              │
      │                          │                        │                       │   mediaType──────────────────────►route to
      │                          │                        │                       │   mediaAlbumArtUri   │           AudioPlayer/
      │── POST Play ────────────►│                        │                       │                     │           ImageViewer/
      │◄─ 200 OK ────────────────│                        │                       │                     │           VideoPlayer
      │                          │── Play ───────────────►│── commandChannel ────►│                     │
      │                          │                        │                       │── playbackState=PLAYING─────────►ExoPlayer.play()
```

---

## Limitations

- **Event push**: `SUBSCRIBE` is acknowledged but PlainApp does not push UPnP events to the controller. Controllers that need position tracking will poll `GetPositionInfo` instead.
- **RenderingControl**: Volume SOAP actions (`SetVolume`, `GetVolume`) return a stub `100`; device volume is not changed.
- **Single renderer**: Only one `DlnaRenderer` instance at a time.
- **Port 7878**: Fixed. If occupied, the server fails silently.
- **subtitle / DRM**: Not supported.


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
