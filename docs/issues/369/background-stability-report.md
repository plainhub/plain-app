# Android background stability — #369

## Scope and reproduced defects

Baseline: upstream `7d8fbf36`. This change improves the lifetime of the user-started
HTTP service; it does not promise an unkillable Android process.

1. Removing the app task stopped the HTTP foreground service. The manifest set
   `stopWithTask=true`; the service also explicitly stopped itself and its SMS
   observer in `onTaskRemoved`. On an Android 12 emulator, task removal changed a
   working HTTP endpoint (200) into a closed connection immediately.
2. After the idle wake lock expired, enabling Keep awake only canceled the timer;
   it did not reacquire the lock. A failing-first test of the actual lock manager
   reproduced this transition.
3. Authenticated browser activity after expiry updated a timestamp without
   reacquiring the lock. A failing-first test reproduced this too.
4. Calling the lock manager's start twice left a collector alive after stop. A
   regression test reproduced a later focus event reacquiring the stopped lock.
   This is a lifecycle robustness defect, not proof that production currently
   calls start twice on the same manager.

## Changes

- The HTTP service outlives task removal. Explicit Stop still tears it down.
- Keep awake and authenticated activity reacquire an expired partial wake lock.
- Start is idempotent. Start, stop, events and timer decisions are serialized;
  lifecycle generations reject stale preference reads/timer work after stop.
- Idle elapsed time uses Android's monotonic clock instead of wall time.
- The existing 30-minute idle policy, USB behavior and explicit stop are preserved.
  There is no added restart alarm, worker, periodic service resurrection, Wi-Fi
  lock, permission, or change of foreground-service classification.

The lock manager accepts clock/event/lock operations for host testing while its
Android constructor supplies the real PowerManager and application event stream.
Tests exercise the production manager, not a duplicate policy implementation.

## Verification

- Failing first: manifest contract failed with `stopWithTask=true`; three of four
  initial lock-manager tests failed before behavior changes.
- `:shared:testAndroidHostTest`: 1,076 tests, zero failures/errors, one skipped
  (1,075 passed), across 101 suites.
- `:app:assembleDebug`: all debug flavors passed. Full tests plus build took 50s
  on the warmed desktop build cache, bounded by a 10-minute timeout.
- Six lock-manager tests cover reacquisition, foreground/USB transitions, explicit
  stop, duplicate start, sliding idle windows, disabling Keep awake, and late
  preference completion after restart. The manifest contract and five existing
  real-server stop-lifecycle tests also pass.
- Installed patched x86_64 Github debug APK in a disposable Android 12 emulator.
  Task removal retained the foreground service and returned HTTP 200 on five
  successive checks. A newly injected SMS reached the browser afterward.
- Incoming SMS also reached the open browser thread with the emulator screen off.
  This short check is not a Doze soak.
- The phone UI's explicit Stop closed the endpoint and released the HTTP wake
  lock. Repeated checks did not observe an immediate restart.

## Tradeoffs and remaining limits

Closing Recents no longer means stopping desktop access: users must select Stop
Service (or use Android's explicit app/service stop controls). Keeping desktop
access active can consume battery, especially with Keep awake enabled. Active
authenticated browser requests now correctly keep a fresh idle window.

Physical-phone follow-up on the owner's Pixel 9 Pro / Android 17 also passed:
after removing both debug activity tasks from Recents, the debug HTTP foreground
service and wake lock remained active. One authorized outgoing SMS was retained
as sent, and both of the owner's replies appeared in the open browser conversation
without a reload. The test used an isolated browser and USB-forwarded connection;
a separate HTTP check over the LAN also returned 200. The phone was USB-powered.

Five further authorized physical-phone SMS exchanges also completed. Sent bubbles
remained visible without Sending indicators, and replies appeared without browser
reloads, including a screen-off round. Browser-offline and deliberately dropped
WebSocket-event rounds recovered successfully. An intended hidden-tab round was
inconclusive as a visibility test: Chromium never reported the tab hidden, although
that message exchange succeeded. No messages were automatically resent.

The owner clarified that PlainApp had been foregrounded during the preceding
five-round run. A separate repeat verified resumed activities before each send,
before each reply, and after recovery: PlainApp was never foregrounded at these
checks. Firefox was observed foregrounded while awake, and the debug HTTP service
remained an Android foreground service. Five outgoing messages were observed in
the business phone's Google Messages web conversation, each followed by exactly
one automated reply. All ten messages persisted in the PlainApp browser without
reloads or stuck Sending indicators. This repeat included a page-scoped network
outage, three suppressed WebSocket frames (recovery about 44.8 seconds), and a
sleep command before the final reply. Display power state during that receipt
was not separately sampled. The phone remained USB-powered/forwarded.

Tests above are not an overnight battery/Doze soak or proof across every OEM.
The physical Pixel's release installation was not replaced. Force-stop and OS
restrictions remain authoritative. Network changes and missed frontend events
also require client recovery; the related #371 frontend work is separate.

Assignment was requested from the owner because self-assignment was denied.

## Upstream integration and download-queue verification

Merged upstream `124e0ccbb` without conflicts. Verification exposed two separate
download-test failures, outside the SMS/background-service changes:

- The fake engine used an unsynchronized mutable map for gates accessed from
  multiple threads. Concurrent creation could release one gate while the engine
  waited on another. Replaced it with atomic `ConcurrentHashMap.computeIfAbsent`,
  and made the other cross-thread test collections safe.
- A task's terminal status could be observed before its engine had returned.
  Re-enqueueing immediately let two queue workers execute the same mutable task,
  with old finalization able to mark the new run failed. A controlled test held
  the first engine at its return boundary and failed on the original queue.

Each dispatch now has an execution identity and predecessor-completion signal.
Successors wait for predecessor cleanup; canceled, removed and superseded entries
cannot start or normalize the newer run's status. Job publication precedes engine
start so cancellation cannot miss a just-starting job. Fresh transport context is
applied when the successor starts, not while the previous engine is still using it.
The registry lock is not held while waiting or invoking completion callbacks.

Dispatch stays ordered even beyond the former channel buffer capacity: an
unbounded channel replaces the old overflow-send coroutines. This does not impose
a new overall queue-size bound (the old registry/overflow coroutines were already
unbounded). The existing three-worker concurrency cap remains; waiting for an old
run's cleanup can occupy a worker until that cleanup finishes.

Regression coverage includes terminal re-enqueue overlap, pause/resume during
cancellation cleanup, removing queued work, a 100-task burst, duplicate rejection,
missing engines, errors/retry, concurrency limiting and progress updates. Channel
waits in the enqueue tests now have explicit deadlines instead of hanging forever.
The combined tree passes all debug APK builds and the full Android host suite:
1,122 passed, one skipped (107 suites). All 15 download tests also passed in five
consecutive forced test reruns, followed by another passing full suite/build.
These are real coroutine/queue tests with
fake transfer engines, not an end-to-end network/download or iOS device test.
