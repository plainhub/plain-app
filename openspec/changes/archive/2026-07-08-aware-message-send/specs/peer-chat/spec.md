## ADDED Requirements

### Requirement: Aware Transport Send Path

`WifiAwareTransport.send(peer, request, keyBytes)` MUST deliver a GraphQL request to a paired peer over a Wi-Fi Aware data path. The data path is built on first discovery of the peer and reused for subsequent sends while the underlying `Network` remains valid. When no live connection exists, `send` rebuilds it on demand.

#### Scenario: First send after discovery
- **WHEN** the Aware subsystem fires `onServiceDiscovered` for a paired peer
- **THEN** `AwareLinkPool.onDiscovered` calls `linkFor(peer).build(peer)` exactly once for that peer within the session
- **AND** both sides' `requestNetwork` calls have a 30-second window to overlap and form the P2P link
- **AND** the resulting `PeerConnection` (Network + peerIpv6 + peerPort + httpClient) is cached on the `AwarePeerLink`

#### Scenario: Reuse cached connection on subsequent send
- **WHEN** `send(peer, ...)` is invoked and `linkFor(peer)` returns an existing `AwarePeerLink` whose `connection` field is non-null
- **AND** `ConnectivityManager.getNetworkCapabilities(connection.network)` returns non-null
- **THEN** `build` returns the cached `PeerConnection` without invoking `requestNetwork`
- **AND** the HTTP POST executes in well under 1 second (LAN-equivalent latency)

#### Scenario: Rebuild on stale Network
- **WHEN** `send(peer, ...)` is invoked and the cached `connection.network` is no longer valid (`getNetworkCapabilities` returns null)
- **THEN** `build` sets the connection reference to null and re-enters the rebuild path
- **AND** a new `requestNetwork` is fired, which pairs with the peer side's listener if it is also waiting

#### Scenario: First send after 60s idle close
- **WHEN** no `send` or `downloadFile` has touched the link for 60 seconds
- **THEN** `AwareLinkPool.sweepIdleLinks` closes the link (`close(reason="idle")`) on the next 10-second sweep tick
- **AND** the next `send(peer, ...)` creates a new `AwarePeerLink` via `linkFor(peer)` and calls `build(peer)`, paying the connect cost again

#### Scenario: Aware session auto-restart on subscribe termination
- **WHEN** AOSP fires `subscribeCallback.onSessionTerminated` for the active `SubscribeDiscoverySession`
- **THEN** `AwareSession` re-invokes `subscribeOwnService(s)` on the still-attached session
- **AND** the next `onServiceDiscovered` for paired peers refills `discoveredPeers` so awaiting `awaitPeerHandle` callers complete

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