# mDNS Responder — Design Notes & Pitfalls

Implementation lives in `app/.../mdns/`. Key files:

| File | Responsibility |
|------|---------------|
| `MdnsHostResponder.kt` | Socket lifecycle, receive loop, send |
| `MdnsIfaceSelector.kt` | Interface enumeration, subnet matching |
| `MdnsPacketCodec.kt` | DNS wire format encode/decode |
| `MdnsReregistrar.kt` | ConnectivityManager callback → restart |
| `MdnsHotspotWatcher.kt` | Hotspot AP state change → restart |
| `NsdHelper.kt` | Entry point called by `HttpServerStartHelper` |

---

## Pitfalls (bugs we fixed and must not re-introduce)

### 1. Response source port MUST be 5353 (RFC 6762 §6.7)

**Bug (v3.0.13–14):** `sendUnicast()` opened a throwaway `DatagramSocket` bound to
`localIp:0`. The kernel assigns a random ephemeral source port.

**Effect:** macOS, Windows, and iOS mDNS resolvers **silently discard** any mDNS
response whose source port ≠ 5353. The device announces itself but nobody can
discover it. Confirmed via Samsung A03s log (SM-A035F, Android 13).

**Fix:** Send via the receive `MulticastSocket` (already bound to port 5353):
```kotlin
s.send(DatagramPacket(response, response.size, senderIp, MDNS_PORT))
```
No separate send socket needed. The kernel routes the unicast reply correctly
because the destination is a specific IP address (not the multicast group).

---

### 2. Bind to explicit IPv4 wildcard, not the default wildcard

**Bug:** `MulticastSocket(MDNS_PORT)` or `bind(InetSocketAddress(MDNS_PORT))` calls
`InetSocketAddress(port)` which resolves to the *system-preferred* wildcard.

**Effect:** On Samsung Android 13+ with `preferIPv6Addresses=true` the socket becomes
`[::]:5353` (IPv6). Joining an IPv4 multicast group (`224.0.0.251`) on an IPv6 socket
**silently fails** — `joinGroup()` returns without error but the socket never receives
mDNS queries.

**Fix:** Explicitly bind to `0.0.0.0`:
```kotlin
bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), MDNS_PORT))
```

---

### 3. Samsung Wi-Fi driver omits IFF_MULTICAST

**Symptom:** `NetworkInterface.supportsMulticast()` returns `false` for `wlan0`/`ap0`
on some Samsung ROMs even though those interfaces work fine for multicast.

**Fix:** Accept any interface whose name starts with `wlan`, `ap`, `eth`, `swlan`,
`wl`, or `p2p` regardless of the `supportsMulticast()` flag (see `candidateInterfaces()`).

---

### 4. joinGroup fallback for kernels that reject per-interface membership

**Symptom:** `joinGroup(SocketAddress, NetworkInterface)` throws on some kernels
(`EINVAL` from `IP_ADD_MEMBERSHIP` with `imr_ifindex`).

**Fix:** If all per-interface `joinGroup` calls fail, fall back to
`joinGroup(InetAddress)` which lets the OS pick the default interface. This is
sufficient for single-interface (non-hotspot) devices.

```kotlin
if (joinCount == 0) {
    runCatching { joinGroup(multicastGroup) }
        .onSuccess { joinCount++ }
}
```

---

### 5. wifiLock.acquire() can throw on some devices

**Symptom:** `WifiManager.WifiLock.acquire()` throws `RuntimeException` on certain
Samsung/MediaTek devices when the Wi-Fi subsystem is in a transient state (Binder
reset, hardware reinit). The crash propagates up through the coroutine.

Stack seen in production:
```
HttpServerLockManager.acquireLocksOnly (HttpServerLockManager.kt:99)
HttpServerLockManager.acquireLocks    (HttpServerLockManager.kt:103)
```

**Fix:** Wrap each `acquire()` in `runCatching`; log failure but do not crash:
```kotlin
runCatching { if (!wifiLock.isHeld) wifiLock.acquire() }
    .onFailure { LogCat.e("WifiLock acquire failed: ${it.message}") }
```

---

## Restart lifecycle

```
HttpServerService.onCreate()
  └─ MdnsReregistrar.start()         registers ConnectivityManager callback
       └─ MdnsHotspotWatcher.start() registers WIFI_AP_STATE_CHANGED receiver

HTTP server ready (handleSuccess)
  └─ NsdHelper.registerServices()
       └─ MdnsHostResponder.start()  binds :5353, joins multicast, starts thread

Network change (any callback fires)
  └─ MdnsReregistrar.schedule()      debounce 2 s, then
       └─ NsdHelper.registerServices() → MdnsHostResponder.start() (stop+restart)

HttpServerService.onDestroy()
  └─ MdnsReregistrar.stop()
  └─ NsdHelper.unregisterService() → MdnsHostResponder.stop()
```

The `MdnsReregistrar` uses a broad `NetworkRequest.Builder().build()` so it fires
for **all** networks (Wi-Fi, Ethernet, VPN), not just the default one. This is
intentional: mDNS must be re-registered whenever any LAN interface changes.

`onCapabilitiesChanged` fires frequently on some devices (every few seconds after
connect). The 2-second debounce in `schedule()` collapses the burst into a single
restart.
