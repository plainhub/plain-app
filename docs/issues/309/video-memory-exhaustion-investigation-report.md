# Web video browsing exhausts Android memory and freezes the device

## Tracking

- Issue: https://github.com/plainhub/plain-app/issues/309
- Repository: https://github.com/plainhub/plain-app
- Branch/commit tested: `upstream/main` at `4ae019bab2cecd212a2628da764409494bcdd7f7`
- Pull request: https://github.com/plainhub/plain-app/pull/353

## Priority and scope

This issue was selected because it can make the entire Android device unresponsive during ordinary browser access, is repeatable, and has no workaround short of force-restarting the phone. The reporter reproduced it across multiple PlainApp releases on Android 12 and specifically identified browsing several videos as the trigger.

The investigation covers the Android HTTP server's `/fs` media-response path, browser range requests, client cancellation, and process resource use. Note/Markdown-editor work and unrelated media-codec compatibility are excluded.

## Reported behavior

After enabling web access and using PlainApp from a PC for one or two minutes, particularly after opening several videos, the reporter's OnePlus NE2210 running ColorOS 12 becomes almost or completely unresponsive. Touch, hardware buttons, charging feedback, and other phone behavior stall until a forced reboot. PlainApp 3.1.6 reduced the observed CPU saturation from approximately 100% to 95% but did not eliminate the severe slowdown.

## Reproduction

### Environment

- Signed production `PlainApp-3.3.16-64bit-Recommended.apk`, version code `673`
- Clean `upstream/main` debug build at `4ae019bab2cecd212a2628da764409494bcdd7f7`
- Android 12/API 31 x86_64 AOSP emulator
- Isolated headless Brave browser controlled through Playwright; the user's normal browser was not used
- Three generated, non-private test videos: 12.36 MB H.264, 100.51 MB H.264, and 41.80 MB HEVC
- ADB TCP forwarding to the emulator's PlainApp HTTP server

### Steps

1. Start PlainApp from a clean process and authenticate the isolated browser client.
2. Open the current bundled web client's Videos page.
3. Repeatedly open video items, allow playback to begin, and close the viewer.
4. Sample `dumpsys meminfo` and process CPU/RSS throughout the run.
5. Repeat with the signed production APK to rule out debug-only instrumentation.

### Actual result

The clean current-upstream debug process starts near 256 MB RSS. Once live `/fs` media responses are exercised, it reaches approximately 6.5 GB RSS within 12 seconds and 9.8 GB total PSS, including 3.0 GB swapped, within about 30 seconds.

The signed production 3.3.16 process starts near 199 MB RSS and reproduces the same failure more quickly:

```text
baseline:  TOTAL PSS 105,289 KB; TOTAL RSS 198,800 KB
~3 sec:    TOTAL PSS 3,081,276 KB; TOTAL RSS 3,177,008 KB
~7 sec:    TOTAL PSS 5,806,966 KB; TOTAL RSS 5,902,704 KB
~15 sec:   TOTAL PSS 8,380,885 KB; TOTAL RSS 7,021,556 KB; swap PSS 1,413,557 KB
final:     TOTAL PSS 9,684,889 KB; TOTAL RSS 6,949,624 KB; swap PSS 2,787,069 KB
```

The emulator's ADB connection briefly went offline under the resulting pressure. This is consistent with the reporter's whole-device freeze.

Nearly all retained memory is reported as `Dalvik Other`, not the Java heap:

```text
Dalvik Heap       16,508 KB PSS
Dalvik Other   6,835,855 KB PSS
Java heap summary 13,684 KB PSS
```

The browser receives valid `206 Partial Content` responses. A single viewer activation initiates full-file media ranges and follow-up seek ranges, for example:

```text
206 Content-Length: 100510409  Content-Range: bytes 0-100510408/100510409
206 Content-Length:  41796857  Content-Range: bytes 0-41796856/41796857
206 Content-Length:  12360504  Content-Range: bytes 0-12360503/12360504
206 Content-Length:     43721  Content-Range: bytes 100466688-100510408/100510409
206 Content-Length: 100444873  Content-Range: bytes 65536-100510408/100510409
```

Closing the viewer aborts browser requests as expected, but the Android process retains the response-buffer allocation and logs `ClosedByteChannelException` / `ChannelWriteException` while memory remains allocated.

### Expected result

Serving and canceling large media range responses must use bounded memory. Closing a browser media viewer must promptly cancel its server-side producer and release buffered response data, leaving the Android device responsive.

## Investigation findings

The failure is current and does not require the reporter's rooted OnePlus, Magisk/LSP environment, large application inventory, or the previously suspected 3C Toolbox APK. It reproduces on a controlled Android 12 emulator with generated media and both debug and signed release builds.

The original disconnected-browser pass did not grow memory; after restoring and verifying the ADB forward with an HTTP 200 response, the same automation immediately reproduced the growth. This isolates live Android media responses as the trigger rather than browser DOM churn.

PlainApp's regular-file path returns a custom `LowMemoryFileContent`, which writes each selected range into Ktor's `ByteWriteChannel` in fixed-size chunks. The source-side byte array remains fixed at 64 KiB, but the process retains gigabytes while browser media ranges are active or canceled. The ordinary Java heap remains small, and the production APK behaves identically, ruling out LeakCanary.

A single direct HTTP `/fs` request that streams and discards the 100.51 MB response reproduces the failure without video decoding, DOM churn, or repeated viewer actions. This exonerates the browser media renderer and localizes the trigger to the Android server's response path.

Replacing the custom response with Ktor's native `LocalFileContent` did not help: ten responses still reached approximately 10.9 GB PSS. Unpooled Netty allocation, increasing the `kotlinx-io` segment pool, and batching Netty flushes also failed to bound memory. These controls rule out PlainApp's 64 KiB file-read buffer, Netty's pooled allocator, the kotlinx-io pool limit, and flush frequency as independent causes.

The process memory map identifies the growth as `[anon:dalvik-LinearAlloc]`, with two mappings alone reaching approximately 3.9 GB and 2.5 GB. A thread dump places the active response workers in Ktor 3.5.2's Netty `respondWithBigBody` path, which repeatedly converts `ByteWriteChannel` segments to Netty buffers. As a control, the same 100.51 MB range completed through Ktor CIO while the app remained near 188 MB PSS. CIO is not a production substitute here because PlainApp serves both HTTP and HTTPS and Ktor's CIO server does not support this HTTPS configuration, but the control isolates the pathological allocation to the Netty big-body bridge on the affected Android runtime.

## Root cause

Browser media elements commonly request an open-ended range such as `Range: bytes=0-`. PlainApp passed the entire remainder of the file through a Ktor `WriteChannelContent` response. With Ktor 3.5.2's Netty engine on Android 12/API 31, the big-body response bridge's per-segment conversion causes pathological `dalvik-LinearAlloc` growth proportional to the response, despite PlainApp reading the file through a fixed 64 KiB byte array. Cancellation does not prevent the process from reaching device-threatening memory pressure quickly enough.

This is why a source implementation that appears bounded still freezes the device: the growth occurs after PlainApp writes each chunk, in the Ktor/Netty engine bridge. Large `/fs` responses trigger it directly; media viewers make it especially visible because they issue open-ended and overlapping seek ranges and may cancel them when the viewer closes.

## Implemented correction

For valid single-range requests made by browser `<video>` or `<audio>` elements, PlainApp now returns at most 4 MiB per `206 Partial Content` response. The bounded slice is read into one byte array and returned with Ktor's small, single-message response path, bypassing the affected Netty big-body bridge. `Content-Range`, `Content-Length`, status, MIME type, and `Accept-Ranges` remain accurate, and browsers request subsequent slices as needed.

The restriction is deliberately narrow:

- It requires `Sec-Fetch-Dest: video` or `Sec-Fetch-Dest: audio` and a valid partial range.
- Requests without `Range`, ordinary downloads, non-media requests, malformed ranges, and multipart ranges keep their previous response behavior.
- Unsatisfiable ranges still return HTTP 416.
- Both PlainApp's HTTP and HTTPS servers continue to use Netty.

HTTP Semantics explicitly permits a `206` response to contain only a subset of the requested range as long as `Content-Range` identifies the enclosed bytes, so the bounded response remains protocol-compliant: https://www.rfc-editor.org/rfc/rfc9110.html#section-15.3.7

## Regression coverage

Nine focused host-side tests were added to the existing range suite. They cover:

- bounding the first and subsequent open-ended video ranges;
- case-insensitive audio range handling;
- preserving small requested ranges;
- leaving ordinary download ranges and requests without `Range` on the existing streaming path;
- preserving existing behavior for malformed and multipart ranges; and
- returning HTTP 416 for unsatisfiable media ranges.

The complete `ResolveSingleByteRangeTest` suite contains 30 passing tests.

## Validation

Post-fix validation used the same Android 12 emulator, generated files, browser page, and request sequence as the baseline:

```text
100.51 MB direct browser-media range:
HTTP 206
Content-Length: 4194304
Content-Range: bytes 0-4194303/100510409
App after response: TOTAL PSS 179,463 KB; TOTAL RSS 269,048 KB; swap PSS 158 KB
```

The real browser `<video>` element followed the bounded responses with consecutive ranges and played all generated H.264 and HEVC samples. A 60-cycle open/play/close stress run completed with every sampled video in playable state (`readyState=4`), no browser console errors, and only expected `net::ERR_ABORTED` request events when the viewer deliberately closed during prefetch. The app remained alive and responsive at approximately 221 MB PSS / 312 MB RSS, versus the 9.7 GB PSS baseline.

An HTTPS media-range request produced the same 4 MiB `206` response and left the app near 223 MB PSS. A non-media 1 MiB range returned the exact requested `bytes 0-1048575` through the existing streaming path. No OOM, ANR, or fatal application exception occurred during the fixed run.

Automated/build validation:

- `./gradlew :shared:testAndroidHostTest --tests com.ismartcoding.plain.httpserver.ResolveSingleByteRangeTest`
- `./gradlew :app:assembleFdroidDebug -PabiFilters=x86_64`
- required all-flavor `./gradlew :app:assembleDebug` — passed

## Remaining limitations

- The reporter's OnePlus/ColorOS hardware is unavailable; the issue is reproduced on an Android 12/API 31 emulator.
- The generated media set is intentionally small and sanitized. It proves the server failure without using private phone media.
- The workaround is scoped to browser media requests because ordinary large downloads retain the current server behavior; replacing or upgrading the affected Ktor/Netty response bridge can be evaluated separately.
- Assignment and pull-request review remain pending.
