# Aware Send Message Simplification — Design

## Context

`docs/wifi-aware-explained.md` (v2026-07-08) explains the three-layer Aware model (Discovery / Provisioning / Application) and the role decision via clientId comparison. `docs/wifi-aware-refactor.md` §12 records two bugs found during integration: (1) `prewarm` success followed by 60s idle close leaves the server side permanently silent, (2) server-side `AwareNetworkInfo.port=0` caused `tryComplete` to never return (fixed by `setPort(peer.port)` in `AwarePeer.connect`).

The current code answers these bugs with:

- A `prewarm` retry loop in `AwarePeerLink` that fires on discovery and keeps trying `build` until success.
- A `prewarmed` flag to keep the sweep from killing successful prewarmed links.
- A `markStale` / `refreshSubscribe` path for the case where `requestNetwork` is silently abandoned by the Aware subsystem.
- A dual-timeout (build 8s vs requestNetwork 30s) so the prewarm can take longer than send.

User feedback: this is too much state. The simplest correct behaviour is: connect on first discovery, keep the link alive, close on 60s idle, rebuild on next send if dead.

## Goals / Non-Goals

**Goals:**
- No proactive `prewarm` loop.
- No `prewarmed` flag, no `isPrewarmed` field.
- Send triggers `build` if no live connection; reuses if `Network` is still valid.
- Idle sweep closes links after 60s with no `lastActiveAt` update.
- One-shot `requestNetwork` on first discovery per peer per session so both sides' provisioning windows overlap.
- Code shrinks: `AwarePeerLink` goes from ~210 lines to ~80.

**Non-Goals:**
- No keep-alive ping. 60s idle means next send pays the connect cost.
- No smart retry inside `build` (no `runCatching { while (true) }`). AOSP's 30s `requestNetwork` timeout is the upper bound.
- No debug UI for Aware state. Removed.
- No change to `AwareSession` role-decision logic, no change to `AwarePeer.connect`, no change to Ktor HTTPS server binding.

## Decisions

### 1. `AwarePeerLink` is reduced to: cache, build, invalidate, close

**Final shape** (~80 lines):

```kotlin
class AwarePeerLink(
    peerId,
    session: AwareSession,
    peer: AwarePeer,                  // isClient decided at create()
    peerInfo: DPeer,
    connectivityManager,
    onClose,
) {
    private val connection = AtomicReference<PeerConnection?>(null)
    private val closed = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val lastActiveAt = AtomicLong(System.currentTimeMillis())

    fun touch() { lastActiveAt.set(System.currentTimeMillis()) }

    suspend fun build(d: DPeer): PeerConnection {
        connection.get()?.let { conn ->
            if (connectivityManager.getNetworkCapabilities(conn.network) != null) {
                return conn
            }
            connection.set(null)
        }
        return withTimeout(BUILD_TIMEOUT_MS) {
            val peerHandle = session.awaitPeerHandle(d.id)
            peer.connect(peerHandle, d.port).also { connection.set(it) }
        }
    }

    fun invalidate() { connection.set(null) }
    fun close(reason: String) { /* unchanged: cancel scope, drop conn, notify onClose */ }
}
```

**`build` semantics:**
- Cache hit: Network still valid → return immediately. No Aware call.
- Cache hit: Network torn down (`getNetworkCapabilities` returns null) → set to null, fall through to rebuild.
- Cache miss: `awaitPeerHandle` (suspend, fires when subscribe discovers this peer) → `peer.connect` (this is the actual `requestNetwork` call) → store, return.

**`build` is the single place where `requestNetwork` happens.** Discovery listener and send both call it; no duplicate orchestration.

**Why `BUILD_TIMEOUT_MS = 25_000` and `REQUEST_TIMEOUT_MS = 30_000`?**
- `requestNetwork` 30s gives both sides enough window to overlap when they call within ~5s of each other.
- `build`'s `withTimeout(25s)` is slightly less so the build coroutine returns a clear `TimeoutCancellationException` if Aware never matched, rather than wait the full 30s Aware timeout.

**Alternatives considered:**
- *Reuse `requestNetwork`'s 5s timeout* (original): empirically not enough. First-send success rate under 50% in tests.
- *No timeout, suspend forever*: bad UX — `send` would hang indefinitely on a peer that left range.
- *Retry on failure*: rejected; the user's feedback explicitly says no retry orchestration.

### 2. `AwareLinkPool` does three things only: hold links, sweep idle, fire one-shot discovery

```kotlin
internal class AwareLinkPool(session, connectivityManager, httpFactory) {
    private val links = ConcurrentHashMap<String, AwarePeerLink>()
    private val subscribedPeers = ConcurrentHashMap<String, DPeer>()
    private val discoveryFired = ConcurrentHashMap<String, AtomicBoolean>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sweepJob: Job? = null

    private val onDiscovered: (String) -> Unit = listener@{ peerId ->
        val fired = discoveryFired.computeIfAbsent(peerId) { AtomicBoolean(false) }
        if (!fired.compareAndSet(false, true)) return@listener
        scope.launch {
            runCatching {
                val peer = subscribedPeers[peerId] ?: return@launch
                val link = linkFor(peer)        // creates or returns existing
                link.build(peer)                // single requestNetwork per session
            }
        }
    }

    fun start() {
        if (sweepJob?.isActive == true) return
        session.setOnServiceDiscovered(onDiscovered)
        sweepJob = scope.launch {
            while (isActive) {
                delay(IDLE_SWEEP_INTERVAL_MS)   // 10s
                sweepIdleLinks()
            }
        }
    }

    fun subscribe(peer: DPeer) {
        subscribedPeers[peer.id] = peer
        session.start()
    }

    fun unsubscribe(peerId: String) {
        subscribedPeers.remove(peerId)
        links.remove(peerId)?.close(reason = "unsubscribe")
    }

    suspend fun linkFor(peer: DPeer): AwarePeerLink {
        session.start()
        return links.computeIfAbsent(peer.id) { AwarePeerLink.create(...) }
    }

    private fun sweepIdleLinks() {
        val now = System.currentTimeMillis()
        links.forEach { (peerId, link) ->
            if (now - link.lastActiveAt.get() > IDLE_TIMEOUT_MS) {  // 60s
                link.close(reason = "idle")
            }
        }
    }
}
```

**Why one-shot (`discoveryFired`)?**
- Aware fires `onServiceDiscovered` once per session per peer — not periodically. AOSP only re-fires on subscribe if the peer disappears and re-appears.
- The one-shot ensures we don't spam `requestNetwork` if the listener is invoked twice for any reason (re-attach, etc.).
- The set is cleared in `stop()` and on session re-attach (not implemented; re-attach invalidates AwareSession internals, so the discovery fires again for the same peer but a new `AtomicBoolean` is allocated).

**Why 10s sweep interval, not 5s?**
- 60s idle with 5s sweep means a link might live 60–65s after its last activity. 10s sweep means 60–70s. The user's requirement is "60s 内没有新动作就断开" — both are within tolerance, and 10s halves the wakeups.

**Why does `start()` register the listener but `subscribe(peer)` only stores the peer?**
- The listener doesn't need a per-peer callback map. It looks up `subscribedPeers[peerId]` at fire time. If the peer isn't subscribed yet (race during cold start), the lookup misses, the launch returns, and the next discovery event re-fires the listener — but `discoveryFired` already locked it as fired. Mitigation: in practice, `PeerStatusManager` calls `notifyAwareOfPairedPeers(peers)` → `WifiAwareTransport.subscribe(peer)` → `pool.subscribe(peer)` synchronously during `ChatManager` startup, so by the time discovery fires, the peer is already in `subscribedPeers`. If not, the one-shot is missed — accepted as a cold-start race; the next send from either side triggers `build` and recovers.

**Alternatives considered:**
- *Re-fire on every discovery event*: more `requestNetwork` calls, may overwhelm AOSP.
- *Re-fire on session re-attach*: would need a hook in `AwareSession.onAwareSessionTerminated`. Not necessary because the user accepts cold-start race.

### 3. `AwareSession` stays minimal; `setOnServiceDiscovered` is the only external hook

`AwareSession` keeps:
- `start()` / `stop()` / `markStale()` / `isAvailable()`
- `awaitReady()` / `awaitPeerHandle(peerId)`
- `setOnServiceDiscribed(listener: (peerId) -> Unit)` — new; the listener is invoked from `onServiceDiscovered` callback in addition to cache/waiter dispatch
- Auto-restart of `subscribe` in `onSessionTerminated` (existing — required for recovery, simple one line)

Removed: `state` flow, `AwareSessionState` data class, `attachedAt` / `publishStartedAt` / `subscribeStartedAt` / `lastDiscoveryAt` timestamps, `discoveryListeners` copy-on-write list (replaced by single `setOnServiceDiscovered`).

**Why single listener, not multi-listener list?**
- Only `AwareLinkPool` needs to react to discovery.
- `CopyOnWriteArrayList<(String) -> Unit>` adds memory + iteration cost for a single user.
- If a second consumer ever needs discovery events, swap the `setOnServiceDiscovered(...)` to a `addDiscoveryListener(...)` then. YAGNI for now.

### 4. `WifiAwareTransport.send` becomes a 4-line call chain

```kotlin
override suspend fun send(peer, request, keyBytes): GraphQLResponse {
    val link = pool.linkFor(peer)            // creates if absent
    val connection = link.build(peer)        // reuses or rebuilds
    link.touch()
    val url = "https://${AWARE_HOST}:${connection.peerPort}/peer_graphql"
    return executeGraphQLRequest(id, peer.id, connection.httpClient, url, request.body, request.channelId)
}
```

No `inFlight` counter, no `try { ... } catch (TransportUnavailable) { link.invalidate(); pool.subscribe(peer); throw }`. `executeGraphQLRequest` throws on HTTP error; that propagates to `PeerTransportRouter` and the next transport (LAN) is tried.

Removed: `debugState: StateFlow<AwarePoolState>`. The dev card on `PeerInfoPage` is removed.

### 5. `PeerInfoPage` dev section removed

The dev card referenced `awareState.session.isAttached / isPublishing / isSubscribing / lastDiscoveryAt / connections`. All gone. `developerMode` toggle stays (used by other dev entries) but the Aware card is deleted.

## Risks / Trade-offs

- **First send after idle takes up to 25s.** User waits. Acceptable per user feedback; alternative was 30s prewarm cost on every subscription.
- **Cold-start race: discovery fires before `subscribedPeers[peerId]` is populated.** Mitigation: `PeerStatusManager` runs `subscribe(peer)` synchronously during `ChatManager.init`, well before the Aware subsystem can fire discovery. If missed, next `send` calls `build` directly, which triggers `awaitPeerHandle` (suspends until the next discovery, which will fire when subscribe stabilises). Worst case: 5–10s extra latency on first send after cold start.
- **`discoveryFired` never reset across session re-attach.** AwareSession auto-restarts subscribe on `onSessionTerminated`; AOSP will fire a fresh `onServiceDiscovered` for already-known peers (because the publish/subscribe sessions are new). But the `AtomicBoolean` in the old map is `true`, so the listener returns early. Mitigation: `stop()` clears `discoveryFired`. Session re-attach does not currently call `stop()` on the pool — it just creates a new AwareSession internally. To make the system robust, future work would clear `discoveryFired` on `onAwareSessionTerminated` via a callback to the pool. Defer (YAGNI) — in practice, session termination is rare and the next `send` triggers `build` directly.
- **AOSP `requestNetwork` 30s timeout is hard-coded.** Cannot be reduced; AOSP default is 30s when no timeout is passed. Our `REQUEST_TIMEOUT_MS = 30_000` matches.
- **LAN fallback unchanged.** P9 has no LAN to P7 in tests (different subnets 192.168.146.x vs 192.168.123.x), so LAN always fails. Aware is the actual primary path. This simplification must keep Aware working, or sends silently degrade.