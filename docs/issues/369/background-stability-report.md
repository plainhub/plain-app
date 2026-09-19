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

Tests above are not an overnight battery/Doze soak or proof across every OEM.
The physical Pixel's release installation was not replaced. Force-stop and OS
restrictions remain authoritative. Network changes and missed frontend events
also require client recovery; the related #371 frontend work is separate.

Assignment was requested from the owner because self-assignment was denied.
