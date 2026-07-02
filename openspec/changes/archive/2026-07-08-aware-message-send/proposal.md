# Aware Send Message Simplification

## Why

`WifiAwareTransport.send` currently sits on top of a `prewarm` retry loop, `prewarmed` flag bookkeeping, idle-sweep exceptions, on-session-terminated auto-restart, liveness checks, and discovery-listener fan-out. None of that is necessary for what we actually need:

1. A data path between paired peers (built once per discovery)
2. Reuse the connection for subsequent sends while it's alive
3. Close it after 60s of idle to free Aware resources
4. Reconnect on next send if the previous connection died

The existing code's prewarm-with-retry was an answer to a real protocol requirement (both sides must call `requestNetwork` for AOSP to pair the P2P link, and the 5s window often didn't overlap), but the answer was over-engineered. A one-shot `requestNetwork` triggered on first discovery, plus an on-demand rebuild on send, is sufficient.

The goal: smallest possible code that still works.

## What Changes

- **Remove `prewarm` loop** from `AwarePeerLink`. Keep only `build(peer)` (suspend) which reuses the cached connection if `Network` is still valid, otherwise rebuilds.
- **Remove `prewarmed` flag** and `AwareConnectionInfo.isPrewarmed`. Connections are either present or not.
- **Remove idle-sweep exception** for "prewarmed" links. Sweep simply closes any link whose `lastActiveAt` is older than 60s — that's the only job it has.
- **One-shot discovery listener** in `AwareLinkPool` calls `build` once per peer per discovery. The `discoveryFired` set guards against the listener firing twice for the same peer within one session.
- **Build timeout 25s, Aware `requestNetwork` timeout 30s.** Send blocks at most ~25s on first connect while AOSP pairs the two requestNetworks. After successful connect, subsequent sends are near-instant (HTTP over cached Network).
- **`send` flow**: `linkFor(peer)` → `build(peer)` → reuse or rebuild → HTTP POST. No prewarm orchestration. No state machines.

## Capabilities

### Modified Capabilities

- `peer-chat` — `WifiAwareTransport.send` now relies on on-demand `build` (rebuilding if `Network` is dead) instead of a maintained prewarm. Idle 60s closes the link and the next send reconnects.

## Impact

- Removes `prewarm`, `prewarmed`, `inFlight`, `isPrewarmed`, liveness-check, refresh-subscribe, and dual-timeout logic. Code shrinks ~50%.
- Idle behavior changes: links close at 60s regardless of state. First send after idle takes up to 25s; subsequent sends reuse.
- Behavior on peer restart: AwareSession auto-restarts `subscribe` on `onSessionTerminated` (kept). Discovery fires fresh → `build` called once → data path re-established.
- Behavior on one-sided restart (only sender restarts): receiver's discovery sees the new sender → receiver calls `build` → server-side `requestNetwork` waits → sender's `send` triggers client-side `requestNetwork` → match within 30s window.
- Behavior on app cold start (both sides restart together): first discovery fires `build` on both sides within ~5s of subscribe starting → both `requestNetwork`s active → match within their overlap window.
- LAN fallback (`LanTransport`) and multicast (`MulticastTransport`) unchanged. Routing in `PeerTransportRouter` unchanged.

## Non-Goals

- Out-of-band keep-alive ping to keep data path warm past 60s idle.
- Dynamic role negotiation beyond the existing clientId comparison (`TempData.clientId < peer.id` → this side is client).
- Multi-peer parallel `requestNetwork` matching (AOSP already supports this; no change).
- Changing the Ktor HTTPS server bind (already `0.0.0.0`, Aware interface falls in automatically per `wifi-aware-refactor.md` §10).
- Debug UI on `PeerInfoPage` — was added in an earlier iteration but `AwareSessionState` and `AwarePoolState` are gone; remove the dev section to keep the page aligned with the simplified transport.