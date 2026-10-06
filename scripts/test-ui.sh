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
say "device=$DEV package=$PACKAGE pid=${PID:-none}"

# The prefs file is the ground truth for what the app persisted. Reading it is
# the only way to catch a format change that would otherwise stay invisible
# until a remote peer fails to verify a signature.
head1 "device identity"
PREFS=$(adb -s "$DEV" shell "run-as $PACKAGE cat files/system_prefs.json" 2>/dev/null | tr -d '\r')
SKP=$(printf '%s' "$PREFS" | grep -o '"signature_key_pair"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1)
if [ -z "$SKP" ]; then
  case_fail "signature_key_pair_stored" "signature_key_pair missing from system_prefs.json"
else
  if printf '%s' "$SKP" | grep -q '{'; then
    PUB=$(printf '%s' "$SKP" | sed -n 's/.*\\"publicKey\\":\\"\([^\\]*\)\\".*/\1/p')
    if [ -n "$PUB" ]; then
      case_pass "signature_key_pair_stored" "JSON keypair form, publicKey present"
    else
      case_fail "signature_key_pair_stored" "JSON present but publicKey is unreadable"
    fi
  else
    case_fail "signature_key_pair_stored" "stored as a bare string, not the JSON keypair the pairing code reads"
  fi
fi
CI=$(printf '%s' "$PREFS" | grep -o '"client_id"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*: *"//;s/"$//')
if [ -n "$CI" ]; then case_pass "client_id_present" "$CI"; else case_fail "client_id_present" "no client_id"; fi

# --- cold start --------------------------------------------------------------
head1 "cold start"
adb -s "$DEV" logcat -c
prepare_device "$DEV"
sleep 3
shot "ui-cold-start"
PID_AFTER=$(adb -s "$DEV" shell pidof "$PACKAGE" | tr -d '\r' | awk '{print $1}')
if [ -z "$PID_AFTER" ]; then
  case_fail "cold_start" "app is not running after launch"
elif [ -n "$PID" ] && [ "$PID_AFTER" != "$PID" ]; then
  case_fail "cold_start" "pid changed $PID -> $PID_AFTER (process restarted during launch)"
else
  case_pass "cold_start" "app alive, pid ${PID_AFTER:-unknown}"
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

# --- login prompt -----------------------------------------------------------
# The 2FA prompt is the one place where a Rust refusal used to kill the app:
# postJson threw on `too_many_login_attempts`, and the host had no handler.
# Tapping Approve three times inside the rate-limit window reproduces it.
head1 "web login prompt"
if [ "${PLAIN_SCREEN_TAPS:-0}" = "1" ]; then
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
  elif [ -n "$PID_AFTER" ] && [ "$PID_LOGIN" != "$PID_AFTER" ]; then
    case_fail "login_prompt_survives" "pid changed $PID_AFTER -> $PID_LOGIN"
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