# Aware Send Message Simplification — Tasks

> Order: top-down. Each task is small and ends with a `BUILD SUCCESSFUL` and a passing stability-test subset where indicated.

- [x] 1. Strip `AwarePeerLink` to build/invalidate/close/touch
  - Remove `prewarm`, `prewarmed`, `inFlight`, `scope.launch` block, `prewarm coroutine`, dual timeout, `MarkStale` handling
  - Keep `connection: AtomicReference<PeerConnection?>`, `closed`, `lastActiveAt`, `touch`, `invalidate`, `close`
  - `build(d: DPeer)`: cache check via `getNetworkCapabilities` → return; else `withTimeout(BUILD_TIMEOUT_MS) { awaitPeerHandle + peer.connect }`
  - `BUILD_TIMEOUT_MS = 25_000L`, single constant
  - File target: `shared/src/androidMain/kotlin/com/ismartcoding/plain/chat/peer/transport/aware/AwarePeerLink.kt`
  - Verify: `./gradlew :shared:compileAndroidMain` BUILD SUCCESSFUL

- [x] 2. Strip `AwarePeer` to one-shot connect with `setPort(peer.port)` already wired
  - `REQUEST_TIMEOUT_MS = 30_000L`
  - No other change; existing role branching (clientId comparison) stays
  - File target: `shared/src/androidMain/kotlin/com/ismartcoding/plain/chat/peer/transport/aware/AwarePeer.kt`
  - Verify: `compileAndroidMain` BUILD SUCCESSFUL

- [x] 3. Strip `AwareSession` of state flow; add `setOnServiceDiscovered`
  - Remove `_state`, `state`, `AwareSessionState`, all `AtomicLong` timestamps, `discoveryListeners` list
  - Add `setOnServiceDiscovered(listener: (peerId: String) -> Unit): Unit` backed by a single `serviceDiscoveredListeners` field (single user, no CopyOnWriteArrayList)
  - `onServiceDiscovered` invokes the listener in addition to cache + waiter
  - Keep `onSessionTerminated` auto-restart of subscribe (one line in subscribe callback)
  - File target: `shared/src/androidMain/kotlin/com/ismartcoding/plain/chat/peer/transport/aware/AwareSession.kt`
  - Verify: `compileAndroidMain` BUILD SUCCESSFUL

- [x] 4. `AwareLinkPool` — one-shot discovery, idle sweep, no `poolState` / `AwareConnectionInfo`
  - Remove `poolState`, `_poolState`, `refreshPoolState`, `AwareConnectionInfo`, `AwarePoolState`
  - Remove `markStale` propagation from `linkFor` `onClose` (no longer needed; rebuild on send handles it)
  - Add `discoveryFired: ConcurrentHashMap<String, AtomicBoolean>` and `onDiscovered` lambda that does one-shot `linkFor(peer).build(peer)`
  - `start()` calls `session.setOnServiceDiscovered(onDiscovered)` once
  - `sweepIdleLinks()`: simple `if (now - lastActiveAt > 60_000) close(reason="idle")` — no `isPrewarmed` exception
  - `IDLE_TIMEOUT_MS = 60_000L`, `IDLE_SWEEP_INTERVAL_MS = 10_000L` (was 5_000)
  - `stop()` clears `discoveryFired`
  - File target: `shared/src/androidMain/kotlin/com/ismartcoding/plain/chat/peer/transport/aware/AwareLinkPool.kt`
  - Verify: `compileAndroidMain` BUILD SUCCESSFUL

- [x] 5. `WifiAwareTransport` — minimal `send` / `downloadFile` / `subscribe` / `unsubscribe`
  - Remove `debugState`, `AwarePoolState` import, `inFlight` calls, `pool.subscribe(peer)` retry-in-catch
  - `send`: `linkFor(peer) → build(peer) → touch → executeGraphQLRequest(...)`
  - `downloadFile`: same shape, throws `TransportUnavailable` on non-2xx (already does)
  - `subscribe(peer)` / `unsubscribe(peerId)` delegate to pool
  - File target: `shared/src/androidMain/kotlin/com/ismartcoding/plain/chat/peer/transport/WifiAwareTransport.kt`
  - Verify: `compileAndroidMain` BUILD SUCCESSFUL

- [x] 6. Remove `PeerInfoPage` Aware dev card
  - Delete `awareState` collection, `LaunchedEffect` polling loop, the `if (developerMode) { item { ... Aware (dev) ... } }` block
  - Keep `developerMode` toggle and the existing peer-info card
  - File target: `app/src/main/java/com/ismartcoding/plain/ui/page/chat/PeerInfoPage.kt`
  - Verify: `./gradlew :app:assembleGithubDebug` BUILD SUCCESSFUL

- [x] 7. Build + install
  - `./gradlew :app:installGithubDebug`
  - Verify: `BUILD SUCCESSFUL` and APK installed on both Pixel 7 (`2C131FDH20090C`) and Pixel 9 (`47260DLAQ003RB`)

- [x] 8. Stability test (apk installed, both apps started, then test sequence)
  - Force-stop + start both apps, wait 20s, set up `adb forward` (P9 → 8888, P7 → 8889)
  - **T1 立即**: curl send from P9 → expect `sent`
  - **T2 30s 后**: curl send → expect `sent` (reuses cached connection)
  - **T3 反向**: curl send from P7 → expect `sent` (other side's link path)
  - **T4 P9 重启**: P9 force-stop + start + 35s wait + send → expect `sent` (on-demand rebuild)
  - **T5 60s 后**: send → expect `sent` (reuses or rebuilds)
  - **T6 P7 重启**: P7 force-stop + start + 35s wait + send → expect `sent`
  - curl template (P9 → P7): `curl -sS --max-time 30 -X POST http://127.0.0.1:8888/graphql -H "c-id: apitestb013" -H "Authorization: Bearer hGNhe+SHsU8Rx0jq/IEwNRCSjs1zHy7g2jrd66iFcfI" -H "Content-Type: application/json" --data '{"query":"mutation { sendChatItem(toId: \"peer:1ib0wx7qexru9\", content: \"{\\\"type\\\":\\\"text\\\",\\\"value\\\":{\\\"text\\\":\\\"tN\\\"}}\") { status } }"}'`
  - Curl template (P7 → P9): same with `127.0.0.1:8889`, `c-id=apitest3695`, token `xsK9ltXMYvC9teonrpIz3jKY0HkN1Di/wLB83HZfD6A`, `toId=peer:t4a27f5gwnz8`

- [x] 9. Update `docs/wifi-aware-refactor.md` §11 to reflect the simplification
  - Mark prewarm / prewarmed / inFlight / liveness check / refreshSubscribe / dual-timeout as removed
  - Mark one-shot discovery listener, build-only, 60s idle sweep, 25s build timeout as final
  - Mark the dev Aware card in `PeerInfoPage` as removed
  - File target: `docs/wifi-aware-refactor.md`

- [ ] 10. User review and commit
  - **DO NOT** auto-commit; wait for user instruction
  - Per user preference rule, no `git commit` without explicit "commit" / "提交" / "push" from user
  - If user says commit, use existing commit message style from `git log` (project uses short imperative subject)