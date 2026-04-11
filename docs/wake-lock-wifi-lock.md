# Wake Lock & Wi-Fi Lock Management

## Overview

`HttpServerLockManager` (`app/.../services/HttpServerLockManager.kt`) manages two Android power locks that keep the device awake and the Wi-Fi radio at full performance while the HTTP server is running:

| Lock | Type | Purpose |
|------|------|---------|
| `WakeLock` | `PARTIAL_WAKE_LOCK` | Prevents CPU from sleeping; screen may turn off |
| `WifiLock` | `WIFI_MODE_FULL_HIGH_PERF` | Keeps Wi-Fi at max throughput with minimal latency |

Both locks are acquired when `HttpServerService` starts and released when it stops.

---

## Business Rules

### 1. USB Charging Connected
- Detected via `PlugInControlReceiver.isUSBConnected(context)` and the `PowerConnectedEvent`.
- When USB is connected: the inactivity timer is **cancelled**; locks are held **indefinitely**.
- When USB is disconnected (`PowerDisconnectedEvent`): `lastActivityMs` is reset to now and the 30-minute inactivity timer **starts**.

### 2. Keep Awake Preference
- User-controlled toggle in **Settings → Web Console → Keep Awake**.
- Stored in `KeepAwakePreference` (DataStore boolean, default `false`).
- When enabled (`KeepAwakeChangedEvent(enabled = true)`): inactivity timer is cancelled; locks held indefinitely (same as USB).
- When disabled (`KeepAwakeChangedEvent(enabled = false)`): `lastActivityMs` reset to now; timer starts.

### 3. 30-Minute Inactivity Timer
Active only when **both** USB is disconnected **and** Keep Awake is disabled.

- A single long-running `inactivityJob` (`coIO`) loops every 60 seconds and checks:  
  `System.currentTimeMillis() - lastActivityMs >= 30 min`
- If the condition is met, `releaseLocks()` is called and the job exits.
- The job is **never restarted** while active; only `lastActivityMs` is updated to slide the window forward.
- `scheduleInactivityTimer()` is a no-op when the job is already active.

### 4. Web Request Activity
- Every authenticated HTTP request fires `WebRequestReceivedEvent`.
- The manager updates `lastActivityMs = System.currentTimeMillis()`, resetting the 30-minute window without touching the timer job.

### 5. App Returning to Foreground
- `WindowFocusChangedEvent(hasFocus = true)` triggers lock re-acquisition (if released) and resets `lastActivityMs`.

---

## State Machine

```
Service start
    └─ acquireLocksOnly()          (sync, called on main thread)
    └─ eventJob starts (coIO)
          └─ read KeepAwakePreference  (async)
          └─ scheduleInactivityTimer()

         ┌──────────────────────────────────────────────┐
         │  Normal state: locks held, timer running      │
         │  WebRequestReceivedEvent → update timestamp   │
         └──────────────────────────────────────────────┘
              │                            │
    USB connected / KeepAwake on     Inactivity timeout
              │                            │
    Cancel timer, hold locks          releaseLocks()
              │
    USB disconnected / KeepAwake off
              │
    Reset timestamp, start timer
              │
    App foreground (WindowFocusChanged)
              │
    Re-acquire locks, reset timestamp
```

---

## Key Implementation Details

### Timestamp vs. Job Restart
High-frequency API calls from a web session would cause constant cancel/restart of the timer job if it were reset per-request. Instead:
- `lastActivityMs` is a `@Volatile Long` updated on each request.
- The timer loop only reads this value — it is never cancelled or restarted from request events.

### `keepAwake` Caching
`BasePreference.getAsync()` is a suspend function; it cannot be called synchronously from a non-coroutine context. The preference is read once at `start()` inside `eventJob` and cached as `@Volatile var keepAwake`. Subsequent changes arrive via `KeepAwakeChangedEvent` and update the cached value.

### acquireLocksOnly vs acquireLocks
- `acquireLocksOnly()` — acquires both locks without scheduling the timer. Used at service start (called on the main thread before `eventJob` is running).
- `acquireLocks()` — acquires then calls `scheduleInactivityTimer()`. Used only from inside `eventJob` where the coroutine context is available.

---

## Event Wiring

| Event | Source | LockManager Response |
|-------|--------|----------------------|
| `WebRequestReceivedEvent` | GraphQL resolver → `sendEvent()` | Update `lastActivityMs` |
| `WindowFocusChangedEvent(hasFocus=true)` | `MainActivity` | Reset timestamp, re-acquire locks |
| `PowerConnectedEvent` | `PlugInControlReceiver` | Cancel timer, re-acquire locks |
| `PowerDisconnectedEvent` | `PlugInControlReceiver` | Reset timestamp, start timer |
| `KeepAwakeChangedEvent(enabled)` | `WebConsoleViewModel` | Toggle lock indefinite/timed mode |

---

## Related Files

| File | Role |
|------|------|
| `services/HttpServerLockManager.kt` | Lock lifecycle and timer logic |
| `services/HttpServerService.kt` | Creates/starts/stops the manager |
| `events/AppEvents.kt` | Event class definitions |
| `preferences/Preferences.kt` | `KeepAwakePreference` definition |
| `preferences/WebSettings.kt` | `LocalKeepAwake` composition local |
| `ui/models/WebConsoleViewModel.kt` | `enableKeepAwake()` → `KeepAwakeChangedEvent` |
| `ui/page/web/WebSettingsPage.kt` | Keep Awake toggle UI |
| `receivers/PlugInControlReceiver.kt` | USB state detection + power events |
