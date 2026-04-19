# mDNS Responder — Design & Compatibility

## File Map

| File | Responsibility |
|------|---------------|
| `MdnsHostResponder.kt` | Socket lifecycle, receive loop, send reply |
| `MdnsIfaceSelector.kt` | Interface enumeration, subnet matching |
| `MdnsPacketCodec.kt` | DNS wire-format encode / decode |
| `MdnsRegister.kt` | ConnectivityManager + hotspot callback → re-register |
| `MdnsHotspotWatcher.kt` | `WIFI_AP_STATE_CHANGED` broadcast → re-register |
| `NsdHelper.kt` | Public entry point, called by `HttpServerStartHelper` |

---

## How It Works

PlainApp runs a custom mDNS **responder** (not a browser). It answers hostname
queries (e.g. `potato.local` → `192.168.1.5`) so other devices can reach the
built-in HTTP server by name.

### Socket setup

1. Create `MulticastSocket(null)` — unbound.
2. Bind to `InetSocketAddress("0.0.0.0", 5353)` — **explicit IPv4 wildcard** (see §Pitfalls).
3. Acquire `WifiManager.MulticastLock` — tells the WiFi driver to forward multicast to this app.
4. `joinGroup(InetSocketAddress(224.0.0.251, 5353), NetworkInterface)` for each LAN interface.
5. Fallback: if all per-interface joins fail (EINVAL), `joinGroup(224.0.0.251)` on default interface.
6. Start a daemon thread that loops on `receive()`.

### Receive loop

- Extract sender IPv4 (unwrap `::ffff:x.x.x.x` if needed).
- Re-fetch `candidateInterfaces()` to pick the correct local IP for the A record.
- `MdnsPacketCodec.buildResponseIfMatch()` — returns `null` if the query is not for our hostname.
- Send response via the **same** MulticastSocket (source port = 5353, RFC 6762 §6.7).
- Log each sent reply for diagnostics.

### Re-registration

`MdnsRegister` watches for network changes and re-registers mDNS when the
interface set changes (IP address assigned/removed, new interface up/down):

```
HttpServerService.onCreate()
  └─ MdnsRegister.start()
       ├─ registers ConnectivityManager.NetworkCallback (onAvailable, onLost, onLinkPropertiesChanged)
       └─ MdnsHotspotWatcher.start() — WIFI_AP_STATE_CHANGED receiver

HTTP server ready
  └─ NsdHelper.registerServices() → MdnsHostResponder.start()

Network change callback fires
  └─ MdnsRegister.schedule()
       ├─ debounce 2 s
       ├─ compare candidateInterfaces() with last registered set
       ├─ skip if unchanged (avoids pointless restarts)
       └─ NsdHelper.registerServices() → stop + start

HttpServerService.onDestroy()
  └─ MdnsRegister.stop()
  └─ NsdHelper.unregisterService() → MdnsHostResponder.stop()
```

**`onCapabilitiesChanged` is intentionally omitted.** It fires for signal-strength
changes, bandwidth estimate updates, and network validation — none of which affect
the interface/IP set. On some Samsung/MediaTek devices it fires every few seconds,
causing pointless mDNS restarts that reduce reliability.

---

## Scenarios

### Pure WiFi

Single interface (`wlan0`). `candidateInterfaces()` returns one entry.
`joinGroup` on `wlan0`, respond with `wlan0`'s IPv4.

### Hotspot (WiFi AP)

Two interfaces: `wlan0` (client WiFi) and `ap0` / `wlan1` (hotspot).
`candidateInterfaces()` returns both. `joinGroup` on each so mDNS queries from
hotspot clients are answered too. `MdnsHotspotWatcher` triggers re-registration
because `ConnectivityManager.NetworkCallback` does **not** fire for hotspot
state changes on the host device.

### VPN

VPN adds `tun0` / `tun1`. These are filtered out by `NetworkHelper.isVpnInterface()`
in `candidateInterfaces()`. mDNS only joins on the physical LAN interfaces.

### Hotspot + VPN

All three types present. VPN excluded, both WiFi and hotspot joined.

---

## Android Multicast — Version & Manufacturer Notes

### Core requirements (all versions)

| Requirement | Detail |
|---|---|
| `MulticastLock` | Must be held or the WiFi driver silently drops multicast (API 4+). |
| Bind address | Must be `0.0.0.0` (IPv4 wildcard), not `InetSocketAddress(port)` which may resolve to `[::]` on some ROMs. |
| Source port | mDNS responses **must** originate from port 5353. Most resolvers (macOS, Windows, iOS) silently discard responses from other ports (RFC 6762 §6.7). |
| `joinGroup` | Calls `IP_ADD_MEMBERSHIP` in the kernel. Per-interface form uses `imr_ifindex`; simple form uses `imr_ifindex=0` (kernel picks default). |

### Per-version notes

| Android | API | Notes |
|---------|-----|-------|
| 9 (Pie) | 28 | Standard `MulticastSocket` + `MulticastLock`. No special handling needed. |
| 10 | 29 | Randomized MAC. No multicast impact. |
| 11 | 30 | No multicast changes. |
| 12 | 31 | `NsdManager` improvements (platform mDNS), but we use custom socket, unaffected. |
| 12L | 32 | No changes. |
| 13 | 33 | `RECEIVER_NOT_EXPORTED` required for broadcast receivers. `MdnsHotspotWatcher` uses this flag. Some Samsung ROMs set `preferIPv6Addresses=true` making `InetSocketAddress(port)` resolve to `[::]` — breaks IPv4 multicast join (see §Pitfalls). |
| 14 | 34 | `FOREGROUND_SERVICE_TYPE` required for foreground services. No multicast API changes. |
| 15 | 35 | Enhanced platform mDNS in `NsdManager`. Our custom socket approach unaffected. |
| 16 | 36 | No multicast API changes. |

### Manufacturer-specific

| Manufacturer | Chip | Known quirks |
|---|---|---|
| **Google Pixel** | Qualcomm | Reference behavior. No known issues. |
| **Samsung (flagship)** | Exynos / Qualcomm WiFi | `supportsMulticast()` may return `false` for `wlan0`/`ap0` — handle via name-based allow-list. `preferIPv6Addresses=true` on some 13+ ROMs — handle via explicit `0.0.0.0` bind. |
| **Samsung (budget)** | MediaTek | Same as above. Budget devices (A03s, A04, etc.) may have more aggressive battery optimization that can delay multicast delivery. Ensure `MulticastLock` is held. |
| **Xiaomi / Redmi** | Qualcomm / MediaTek | Generally standard. MIUI battery saver can kill background services but foreground service with lock is fine. |
| **Huawei / Honor** | Kirin / Qualcomm | Standard multicast behavior. EMUI power management same caveat as Xiaomi. |
| **OnePlus / Oppo / Realme** | Qualcomm / MediaTek | Standard. ColorOS battery optimization same caveat. |

### Common misconceptions

- **"Per-interface `joinGroup` is broken on Samsung"** — Unconfirmed. The log from
  SM-A035F shows `joinGroup` succeeding without error. The responder thread starts
  normally. Without receive-loop diagnostics, we cannot conclude whether the issue is
  multicast reception, hostname mismatch, or external (router AP isolation, network
  topology). The response log (`mDNS reply ...`) added in the receive loop will
  clarify this in future reports.

- **"Unconditional default-interface join fixes Samsung"** — Unproven. Adding
  redundant `joinGroup` calls makes the code harder to reason about and may cause
  double IGMP reports. The conditional fallback (only when per-interface joins all
  fail) is the correct approach.

---

## Pitfalls (bugs fixed — must not re-introduce)

### 1. Response source port must be 5353 (RFC 6762 §6.7)

**Bug:** Sending via a throwaway `DatagramSocket` bound to `:0` assigns a random
ephemeral source port. macOS/Windows/iOS resolvers silently discard the response.

**Fix:** Send via the receive `MulticastSocket` (already bound to port 5353).

### 2. Bind to explicit `0.0.0.0`, not the default wildcard

**Bug:** `MulticastSocket(port)` or `InetSocketAddress(port)` uses the system-preferred
wildcard. On Samsung Android 13+ with `preferIPv6Addresses=true`, this becomes `[::]`.
Joining an IPv4 multicast group on an IPv6 socket silently fails.

**Fix:** `bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), 5353))`

### 3. Samsung Wi-Fi driver omits IFF_MULTICAST

**Symptom:** `supportsMulticast()` returns `false` for `wlan0`/`ap0` on some Samsung ROMs.

**Fix:** Allow interfaces with LAN-like names (`wlan*`, `ap*`, `eth*`, `swlan*`, `wl*`, `p2p*`)
regardless of the flag.

### 4. Some kernels reject per-interface `joinGroup` (EINVAL)

**Symptom:** `joinGroup(SocketAddress, NetworkInterface)` throws `EINVAL` because the
kernel's `IP_ADD_MEMBERSHIP` codepath doesn't accept a non-zero `imr_ifindex`.

**Fix:** Fall back to `joinGroup(InetAddress)` (default interface) when all per-interface
joins fail. Sufficient for single-interface (non-hotspot) devices.

### 5. `WifiLock.acquire()` can throw on transient hardware states

**Symptom:** `RuntimeException` on Samsung/MediaTek during WiFi subsystem reset.

**Fix:** Wrap in `runCatching`; log failure but don't crash.

### 6. `lastRegisteredIfaces` not reset on failed re-register — stale state prevents recovery

**Symptom:** Phone leaves WiFi (e.g. user goes out). `onLost` fires → `schedule()` calls
`NsdHelper.registerServices()` which first calls `unregisterService()` (destroying the
running responder), then `MdnsHostResponder.start()` fails because there are no interfaces.
`registerServices()` returns `false`, but `lastRegisteredIfaces` is NOT reset — it still
holds the old interface set (e.g. `{wlan0:192.168.123.24}`). When the phone reconnects to
the same WiFi with the same IP, `candidateInterfaces()` returns `{wlan0:192.168.123.24}`
which equals `lastRegisteredIfaces` → the re-register is **skipped**. The responder was
already destroyed during the `onLost` handling, so mDNS is permanently dead until the app
is restarted.

**Fix (two parts):**
1. When `currentIfaces` is empty (network gone), call `NsdHelper.unregisterService()`
   directly and reset `lastRegisteredIfaces = emptySet()` without going through
   `registerServices()` (which has the destructive side-effect of tearing down the
   responder before checking if interfaces exist).
2. When `registerServices()` returns `false` or throws, reset `lastRegisteredIfaces =
   emptySet()` so the next network-up event always triggers a fresh registration attempt.

**Log pattern (before fix):**
```
mDNS re-register (onLost): []
mDNS: no candidate interfaces found
Network change (onLinkPropertiesChanged) — restarting multicast listener
<no "mDNS re-register" log — skipped because lastRegisteredIfaces matched>
```

### 7. `onCapabilitiesChanged` causes excessive mDNS restarts

**Symptom:** On some Samsung/MediaTek devices, `onCapabilitiesChanged` fires every
few seconds (signal strength, bandwidth estimates). Each callback triggers an mDNS
stop + start cycle, creating windows where the responder can't answer queries.

**Fix:** Removed `onCapabilitiesChanged` from the `NetworkCallback`. Only
`onAvailable`, `onLost`, and `onLinkPropertiesChanged` are relevant for interface/IP
changes. Additionally, `MdnsRegister.schedule()` compares the current interface set
with the last registered set and skips re-registration when nothing changed.

---

## Debugging

When a user reports "mDNS not working", check the log for:

1. `mDNS joined <iface> (<ip>)` — did the multicast group join succeed?
2. `mDNS responder started for <hostname> on N interface(s)` — is the responder running?
3. `mDNS reply <hostname> → <ip> to <querier>` — is the responder answering queries?
4. `mDNS re-register (...)` — is the responder being restarted excessively?

If (1) and (2) succeed but (3) never appears, possible causes:
- **Router AP isolation**: the router blocks multicast between WiFi clients.
- **MulticastLock not held**: check for lock acquisition errors in the log.
- **Hostname mismatch**: the querier is looking for a different hostname.
- **Multicast not delivered**: rare driver-level issue; ask user to test with
  `adb shell ping -I wlan0 224.0.0.251` to verify kernel multicast routing.
