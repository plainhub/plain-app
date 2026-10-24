# Opening a native resource stream replaces the host connection

Date: 2026-10-23
Area: Android app — `plain-rs` JNI glue, `shared` native stream pollers
Base: `plain-app` `f175f9f98`, `plain-core` `34a9d89`

## Symptom

Opening an SMS conversation in the web UI returned **"Host request queue
unavailable"** and the Android process crashed. `crash_log.txt` recorded three
shapes of failure:

- `IllegalStateException: Rust event stream closed` from `RustEventHandler` and
  `RustHostApiHandler` (process killed).
- `IllegalArgumentException: host disconnected` from `sendEventToRust`, called by
  `RustNotificationEvents.publish` and `AppEvents.register`.

The native failure before the first crash was only visible as
`Rust panic: non-string panic payload`: `jni` 0.22's `ThrowRuntimeExAndDefault`
policy only downcasts `&'static str`, so formatted panic payloads lose their
message.

## Root cause

A temporary panic hook (diagnostic only, not part of this change) captured the
real backtrace:

```text
plain-core/plain-server/src/services/settings/update_download.rs:90
there is no reactor running, must be called from the context of a Tokio 1.x runtime
  7: tokio::task::spawn::spawn_inner::<...update_download::Runtime::schedule_reconcile...>
  8: <plain_server::assembly::server::ContentServer>::host_events
  9: plain_rust::ffi::resources::open
 10: Java_com_ismartcoding_plain_rust_native_RustNativeApi_resourceOpenNative
```

`plain-rs/src/ffi/resources.rs::open` called `ContentServer::host_events()` from
a JNI thread. That is not a lookup: `HostEvents::new` calls
`state.host.connect()`, which

- increments the host connection generation,
- installs a new request sender,
- drains pending requests with `"host connection replaced"`, and
- notifies `connection_changes()`, which makes the existing `HostEvents`
  (the one the event/host pollers use) report itself disconnected.

`HostEvents::new` also spawns maintenance work with `tokio::spawn`
(`update_download::Runtime::schedule_reconcile`, mDNS/casting
`host_connected`), which panicked because the JNI thread was not inside the
core runtime.

So every resource open — e.g. the avatars/attachments of an SMS conversation —
created a second host connection, tore down the pollers' one, and then crashed
on either the off-runtime `tokio::spawn` or the following
`"host disconnected"` sends.

## Fix

- `resources::open` now reuses the singleton `HostEvents` installed by
  `events::install` (via `events::bridge`) instead of creating a new one. This
  removes both the off-runtime spawn and the connection churn.
- Both pollers catch a closed/failed stream, log it, and end, instead of letting
  the exception escape an uncaught coroutine. `readRustStream` keeps propagating
  so its cleanup contract and existing cancellation test are unchanged.
- `sendEventToRust` logs a failed host send instead of `require`-throwing. Host
  events are best-effort; a stopped or reconnecting core must not kill a
  background caller.

## Verification

- `./gradlew :app:assembleDebug` passes.
- Reproduced on a physical Pixel 9 Pro (debug `4.0.0`/`706`): loading an SMS
  conversation produced the toast, the native failure and the crash above.
- After the change the debug build installs and launches with no native panic
  and no host-disconnect crash. The web SMS conversation retest is the remaining
  acceptance check.

## Follow-up

`ContentServer::host_events()` looks like a cheap getter but replaces the host
connection. Making that explicit (or spawning through a stored `Handle` and
sharing a single `HostEvents`) would prevent the same misuse at future call
sites.
