#!/bin/bash
# UI tests — drive a real phone through the flows that have broken before,
# capturing a screenshot at each step and asserting the app survives.
#
#   ./scripts/test-ui.sh                 # auto-pick Pixel 7
#   ./scripts/test-ui.sh <adb-serial>    # or pass one explicitly
#   PLAIN_SCREEN_TAPS=1 ./scripts/test-ui.sh   # include coordinate taps
#
# What is asserted here is crash-freedom and identity consistency, not visual
# layout. The per-screen visual walk is the manual checklist in
# `test-reports/TEST_CHECKLIST.md`; coordinate taps cannot tell a correct screen
# from a wrong one, so this script never claims a screen "looks right".
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"

MODEL="${PLAIN_DEVICE_MODEL:-Pixel_7}"
SHOTS="${PLAIN_SHOTS_DIR:-$REPO_ROOT/test-reports/shots}"
DEV="${1:-}"
[ -z "$DEV" ] && DEV=$(serial_for_model "$MODEL")

if ! have_adb; then
  say "!! adb reports no connected device — connect one and retry"
  report_write; exit 1
fi
if [ -z "$DEV" ]; then
  say "!! no device matched model:${MODEL}; set PLAIN_DEVICE_MODEL or pass a serial"
  report_write; exit 1
fi
mkdir -p "$SHOTS"

shot() {
  local name="$1"
  adb -s "$DEV" shell screencap -p /sdcard/_shot.png >/dev/null 2>&1
  adb -s "$DEV" pull /sdcard/_shot.png "$SHOTS/$name.png" >/dev/null 2>&1 \
    && say "  shot -> $SHOTS/$name.png" || say "  shot FAILED: $name"
}

crashes() {
  adb -s "$DEV" logcat -d 2>/dev/null \
    | grep -E "FATAL EXCEPTION|AndroidRuntime: *Process: $PACKAGE" | head -5
}

PID=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
say "device=$DEV package=$PACKAGE pid-before=${PID:-none}"

# The prefs file is the ground truth for what the app persisted. Reading it is
# the only way to catch a format change that would otherwise stay invisible
# until a remote peer fails to verify a signature.
head1 "device identity"
PREFS=$(adb -s "$DEV" shell "run-as $PACKAGE cat files/system_prefs.json" 2>/dev/null | tr -d '\r')
SKP=$(printf '%s' "$PREFS" | grep -o '"signature_key_pair".*' | head -1)
if [ -z "$SKP" ]; then
  case_fail "signature_key_pair_stored" "signature_key_pair missing from system_prefs.json"
elif ! printf '%s' "$SKP" | grep -q 'publicKey'; then
  # The value is a bare string, not the {"privateKey","publicKey"} object the
  # pairing and login code both parse. Signatures then come out empty.
  case_fail "signature_key_pair_stored" "not stored as a JSON keypair; readers that parse the object will fail"
elif ! printf '%s' "$SKP" | grep -q 'privateKey'; then
  case_fail "signature_key_pair_stored" "JSON present but privateKey is missing"
else
  case_pass "signature_key_pair_stored" "JSON keypair with both keys"
fi
CI=$(printf '%s' "$PREFS" | grep -o '"client_id"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*: *"//;s/"$//')
if [ -n "$CI" ]; then case_pass "client_id_present" "$CI"; else case_fail "client_id_present" "no client_id"; fi

# --- cold start --------------------------------------------------------------
head1 "cold start"
adb -s "$DEV" logcat -c
prepare_device "$DEV"
sleep 3
shot "ui-cold-start"
# prepare_device force-stops the app on purpose, so the pid is expected to
# change here. What must not happen is a *further* restart: read the pid after
# launch, then again once the UI has settled.
PID_AFTER=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
if [ -z "$PID_AFTER" ]; then
  case_fail "cold_start" "app is not running after launch"
else
  sleep 4
  PID_SETTLED=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
  if [ -z "$PID_SETTLED" ] || [ "$PID_SETTLED" != "$PID_AFTER" ]; then
    case_fail "cold_start" "pid changed $PID_AFTER -> ${PID_SETTLED:-gone} while the UI settled"
  else
    case_pass "cold_start" "app alive and stable, pid $PID_AFTER"
  fi
fi
OUT=$(crashes)
if [ -z "$OUT" ]; then case_pass "cold_start_no_crash" "no FATAL EXCEPTION"
else case_fail "cold_start_no_crash" "$(printf '%s' "$OUT" | tr '\n' ' ')"; fi

# --- service start -----------------------------------------------------------
# "Start Service" sits at (540,763) on a 1080x2400 screen. The first-run
# wizard can cover it, so the tap is repeated after the permission settle.
head1 "http service"
IP="$(device_ip "$DEV")"
say "  ip=${IP:-unknown}"
if [ "${PLAIN_SCREEN_TAPS:-0}" = "1" ]; then
  adb -s "$DEV" shell input tap 540 763; sleep 6
  adb -s "$DEV" shell input tap 540 763; sleep 6
  shot "ui-service-started"
fi
if wait_for_health "$IP" 8080 12; then
  case_pass "service_serving" "http://${IP}:8080/health -> 200"
else
  case_block "service_serving" "service not answering on ${IP}:8080 (start it from the Home page)"
fi

# --- files screen ------------------------------------------------------------
# Opening Files crashed once on each device with
#   IllegalArgumentException: Invalid token size
# from DocMediaStoreHelper.getDocExtGroupsAsync, and did not reproduce on the
# attempts right after. An intermittent crash that a walk happened to catch is
# worth re-running on every gate, so this opens the screen a few times and
# fails on any new crash record rather than only on the one that shows up.
head1 "files screen"
a() { adb -s "$DEV" "$@"; }
BEFORE=$(a shell "run-as $PACKAGE cat files/crash_log.txt" 2>/dev/null | grep -c "IllegalArgumentException")
CRASHED=""
if [ "${PLAIN_SCREEN_TAPS:-0}" = "1" ]; then
  for n in 1 2 3; do
    a shell am force-stop "$PACKAGE"; sleep 1
    a shell am start -n "$PACKAGE/com.ismartcoding.plain.MainActivity" >/dev/null 2>&1; sleep 4
    a shell input tap 677 2211; sleep 2   # Tools
    a shell input tap 230 1062; sleep 5   # Files
    NOW=$(a shell "run-as $PACKAGE cat files/crash_log.txt" 2>/dev/null | grep -c "IllegalArgumentException")
    if [ "$NOW" -gt "$BEFORE" ]; then CRASHED="$CRASHED attempt$n"; fi
  done
  if [ -n "$CRASHED" ]; then
    shot "ui-files-crash"
    case_fail "files_screen" "crashed on:$CRASHED"
  else
    case_pass "files_screen" "opened 3 times with no new crash record"
  fi
else
  case_block "files_screen" "set PLAIN_SCREEN_TAPS=1 to drive the navigation"
fi

# --- login prompt -----------------------------------------------------------
# The 2FA prompt is the one place where a Rust refusal used to kill the app:
# postJson threw on `too_many_login_attempts`, and the host had no handler.
# Tapping Approve three times inside the rate-limit window reproduces it.
head1 "web login prompt"
if [ "${PLAIN_SCREEN_TAPS:-0}" = "1" ]; then
  # The files block above restarts the app on purpose, so the pid to compare
  # against is the one from just before these cycles, not the cold-start one.
  PID_LOGIN_BASE=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
  # Approve whatever prompt is on screen. Each tap needs a fresh browser login
  # to produce one — approve once per attempt and watch the whole window.
  for i in 1 2 3; do
    say "  login cycle $i — submit the login form in the browser, then approve here"
    sleep 8
    adb -s "$DEV" shell input tap 540 1588 >/dev/null 2>&1   # Allow
    sleep 2
    shot "ui-login-cycle-$i"
  done
  PID_LOGIN=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
  if [ -z "$PID_LOGIN" ]; then
    case_fail "login_prompt_survives" "app died during repeated login approvals"
  elif [ -n "$PID_LOGIN_BASE" ] && [ "$PID_LOGIN" != "$PID_LOGIN_BASE" ]; then
    case_fail "login_prompt_survives" "pid changed $PID_LOGIN_BASE -> $PID_LOGIN"
  else
    case_pass "login_prompt_survives" "app alive through repeated approvals"
  fi
else
  case_block "login_prompt_survives" "set PLAIN_SCREEN_TAPS=1 and log in from a browser to exercise this"
fi

# --- crash log ---------------------------------------------------------------
# The app writes its own crash_log.txt; it survives a process restart, so an
# old entry must not be reported as this run's result.
head1 "crash log"
CL=$(adb -s "$DEV" shell "run-as $PACKAGE cat files/crash_log.txt" 2>/dev/null | tr -d '\r')
if [ -z "$CL" ]; then
  case_pass "no_recorded_crash" "no crash_log.txt on device"
else
  FIRST=$(printf '%s' "$CL" | head -1)
  case_block "no_recorded_crash" "crash_log.txt exists (first line: $FIRST) — check the run time against it"
fi

report_write