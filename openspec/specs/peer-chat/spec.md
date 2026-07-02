# peer-chat Specification

## Purpose
TBD - created by archiving change aware-message-send. Update Purpose after archive.
## Requirements
### Requirement: Aware Transport Single Data Path

For each paired peer, exactly one Wi-Fi Aware data path is established between the two devices. The data path is bidirectional; both ends MAY send HTTP requests over the same path. A second data path to the same peer MUST NOT be opened while the first is live.

#### Scenario: One data path per peer
- **WHEN** a paired peer is reachable over Wi-Fi Aware
- **THEN** at most one `PeerConnection` exists for that peer across both devices
- **AND** HTTP traffic in either direction uses the same data path

#### Scenario: Bidirectional over one path
- **GIVEN** a peer link in `CONNECTED` state with Network `N`
- **WHEN** either device makes an HTTP request to the other
- **THEN** the request traverses `N`
- **AND** the response traverses `N` back

### Requirement: Aware Transport Discovery

When `onServiceDiscovered(peerHandle)` fires for a paired peer, the link layer updates its cached `PeerHandle` for that peer and issues a one-shot `build(peer)` to open the data path. AOSP only pairs a Wi-Fi Aware data path when BOTH the subscriber (client) and publisher (server) have called `connectivityManager.requestNetwork`; triggering `build` on discovery ensures both sides call `requestNetwork` without waiting for an explicit `send` from either side. The build's `Mutex`-guarded state machine prevents duplicate `requestNetwork` calls: `CONNECTED` returns the cached `PeerConnection`, `CONNECTING` waits for the in-flight build, `IDLE` starts a new build.

#### Scenario: Discovery updates peerHandle and triggers build
- **WHEN** `AwareSession.subscribeCallback.onServiceDiscovered(peerHandle, ...)` fires for peer `P`
- **THEN** `AwareLinkPool.onPeerDiscovered(P, peerHandle)` updates `AwarePeerLink(P).peerHandle` to the new handle
- **AND** `AwarePeerLink.build(P)` is called once to issue `connectivityManager.requestNetwork`
- **AND** no prewarm coroutine is launched

#### Scenario: Latest peerHandle always used
- **WHEN** discovery fires twice for the same peer in one discovery session
- **THEN** the second `peerHandle` replaces the first on the link
- **AND** the next `build` uses the latest handle
- **AND** stale peerHandles from previous discovery sessions are never reused

### Requirement: Aware Transport Build State Machine

`AwarePeerLink.build(peer)` is the single entry point that opens a data path. It uses a `Mutex` and an explicit state machine to serialize concurrent calls and prevent re-entry. Valid transitions are `IDLE → CONNECTING → CONNECTED` and `CONNECTED → IDLE`. Any other transition is forbidden.

#### Scenario: IDLE starts a build
- **WHEN** `build(peer)` is called and the link is in `IDLE`
- **THEN** the call acquires the `Mutex`, sets state to `CONNECTING`, calls `connectivityManager.requestNetwork` once, sets state to `CONNECTED` on `onAvailable`, and returns the `PeerConnection`
- **AND** no second `requestNetwork` is issued while the first is pending

#### Scenario: CONNECTING waits for in-flight build
- **WHEN** `build(peer)` is called concurrently with another `build(peer)` on the same link
- **THEN** the second caller blocks on the `Mutex` until the first call completes
- **AND** the second caller observes the final state of the first call (`CONNECTED` returns the cached `PeerConnection`; `IDLE` starts a new build)

#### Scenario: CONNECTED returns cached connection
- **WHEN** `build(peer)` is called and the link is in `CONNECTED`
- **AND** `connectivityManager.getNetworkCapabilities(connection.network)` returns non-null
- **THEN** `build` returns the cached `PeerConnection` without calling `requestNetwork`

#### Scenario: CONNECTED rebuilds on stale Network
- **WHEN** `build(peer)` is called and the link is in `CONNECTED`
- **AND** `connectivityManager.getNetworkCapabilities(connection.network)` returns null
- **THEN** `build` clears the cached connection, transitions to `IDLE`, and proceeds with a fresh build

#### Scenario: CLOSED rejects build
- **WHEN** `build(peer)` is called and the link is in `CLOSED`
- **THEN** `build` throws `IllegalStateException` and does not change state

### Requirement: Aware Transport NetworkCallback Lifecycle

Every `connectivityManager.requestNetwork` call MUST be paired with exactly one `connectivityManager.unregisterNetworkCallback` call. The callback is unregistered when the data path is lost (`onLost`), when AOSP rejects the request (`onUnavailable`), or when the build coroutine is cancelled before either fires.

#### Scenario: onLost unregisters callback
- **WHEN** AOSP fires `NetworkCallback.onLost(network)` for the active data path
- **THEN** the callback unregisters itself via `unregisterNetworkCallback(this)`
- **AND** the link transitions to `IDLE`
- **AND** the cached `OkHttpClient` connection pool is evicted
- **AND** the cached `PeerConnection` is cleared

#### Scenario: onUnavailable unregisters callback
- **WHEN** AOSP fires `NetworkCallback.onUnavailable` after the 30s timeout
- **THEN** the callback unregisters itself
- **AND** `build` throws `IllegalStateException("network unavailable")`

#### Scenario: Cancellation unregisters callback
- **WHEN** the build coroutine is cancelled (by `withTimeout` or caller cancellation) before `onAvailable`, `onUnavailable`, or `onLost` fires
- **THEN** the callback is unregistered before the exception propagates

#### Scenario: No callback leak across rebuilds
- **WHEN** the link transitions `CONNECTED → IDLE → CONNECTING → CONNECTED` (rebuild after loss)
- **THEN** the previous callback has already been unregistered
- **AND** the new callback is registered exactly once for the new build
- **AND** the global "registered minus unregistered" counter remains bounded by the number of live links

### Requirement: Aware Transport Initiator Selection

Exactly one side initiates pairing per peer pair. The initiator role is determined deterministically by `TempData.clientId < peer.id`: the side with the smaller clientId is the client (initiator). This guarantees that AOSP pairs exactly one requestNetwork from each side rather than racing two client requests.

#### Scenario: Smaller clientId initiates
- **WHEN** device A has `clientId=A_id` and device B has `clientId=B_id` with `A_id < B_id`
- **THEN** device A is the initiator (`isClient=true`)
- **AND** device A's `requestNetwork` uses the subscribe session plus the peer handle
- **AND** device B's `requestNetwork` uses the publish session plus the LOCAL listening port (`TempData.httpsPort.value`), not the remote peer's port
- **AND** AOSP pairs them as soon as both sides have called `requestNetwork`

#### Scenario: No concurrent initiator builds on same pair
- **WHEN** both devices attempt to build at the same time after discovery
- **THEN** the AOSP protocol itself prevents two client specifiers from pairing each other
- **AND** the per-link `Mutex` prevents a single device from issuing two `requestNetwork` calls on the same link

### Requirement: Aware Transport HTTP Client TLS

Both the GraphQL client (`AwareHttpClientFactory.build`) and the file download client (`AwareHttpClientFactory.buildFileDownload`) MUST trust the peer's self-signed TLS certificate. The file download client MUST be built from `createUnsafeOkHttpClient()` (or equivalent trust-all configuration) so that the TLS handshake succeeds against the same self-signed cert used by the Ktor HTTPS server. A bare `OkHttpClient.Builder()` without an `ignoreAllSSLErrors`/trust-all configuration MUST NOT be used, because it rejects the self-signed cert and `downloadFile` fails with an SSL exception.

#### Scenario: File download client trusts self-signed cert
- **WHEN** `AwareHttpClientFactory.buildFileDownload(network, peerIpv6)` builds the download client
- **THEN** the client uses a trust-all `SSLSocketFactory` (inherited from `createUnsafeOkHttpClient`)
- **AND** the client's `socketFactory` is set to `network.socketFactory` to route TCP through the Aware data path
- **AND** the client's `hostnameVerifier` accepts the `plain-aware-peer` pseudo-hostname

### Requirement: Aware Transport Reconnection

After a data path is lost (`onLost`) or after `idle` close by `sweepIdleLinks`, the next `build(peer)` call MUST establish a fresh data path using a fresh `PeerHandle` and a fresh `NetworkCallback`. The link MUST NOT reuse any `PeerConnection`, `Network`, `PeerHandle`, or `NetworkCallback` from the previous data path.

#### Scenario: Rebuild after onLost
- **GIVEN** the link transitioned `CONNECTED → IDLE` via `onLost`
- **WHEN** the next `build(peer)` is called
- **THEN** the cached `connection` is null and state is `IDLE`
- **AND** the build uses the latest `peerHandle` from `updatePeerHandle` (which may have been refreshed since the previous build)
- **AND** a new `requestNetwork` is issued with a new `NetworkCallback`

#### Scenario: Rebuild after 60s idle close
- **WHEN** no `send` or `downloadFile` has touched the link for 60 seconds
- **THEN** `AwareLinkPool.sweepIdleLinks` closes the link on the next 10-second sweep tick
- **AND** the next `send(peer, ...)` creates a new `AwarePeerLink` via `linkFor(peer)` and calls `build(peer)`
- **AND** the new build uses the latest `peerHandle` from the most recent `onServiceDiscovered`

### Requirement: Aware Transport Idle Sweep

`AwareLinkPool.sweepIdleLinks` closes any link whose `lastActiveAt` is older than 60 seconds. There is no exception list: idle close applies to every link uniformly.

#### Scenario: 60s idle closes any link
- **WHEN** `now - link.lastActiveAt.get() > 60_000ms` for any link
- **THEN** `sweepIdleLinks` calls `link.close(reason="idle")`
- **AND** the next `send(peer, ...)` rebuilds the link from `IDLE`

### Requirement: Aware Transport State Surface

`WifiAwareTransport` MUST NOT expose a debug state flow or per-link `isPrewarmed` field. The transport's only mutable state is the link cache in `AwareLinkPool` and the `lastActiveAt` timestamp on each `AwarePeerLink`.

#### Scenario: No AwarePoolState
- **WHEN** any consumer reads `WifiAwareTransport.debugState`
- **THEN** the call MUST NOT compile (the property is removed)

#### Scenario: No prewarmed flag
- **WHEN** `AwareLinkPool.sweepIdleLinks` evaluates a link
- **THEN** it MUST NOT consult any `isPrewarmed` field
- **AND** it closes the link solely based on `now - lastActiveAt > 60_000ms`

### Requirement: Aware PeerInfo Debug Card

`PeerInfoPage` MUST NOT display a Wi-Fi Aware subsystem state card under `developerMode`. The `developerMode` toggle MAY remain for other dev entries.

#### Scenario: Aware card removed
- **WHEN** `PeerInfoPage` is rendered with `developerMode = true`
- **THEN** the page MUST NOT contain any list item with title "attached", "publish", "subscribe", "last discovery", "connection to this peer", "peer ipv6", "peer port", "last active", or "all connections"
- **AND** the page MUST NOT collect from `WifiAwareTransport.debugState`