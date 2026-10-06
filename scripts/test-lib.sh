#!/bin/bash
# Shared helpers for the plain-app test scripts.
#
# Every script sources this, then calls `report_init`, emits cases with
# `case_pass` / `case_fail` / `case_block`, and finishes with `report_write`.
# `report_write` prints the human summary, exits non-zero when a gate failed,
# and drops a `report.json` when `--output <dir>` was given so the workbench
# /tests page can render the same cases.

set -uo pipefail

export PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$HOME/.cargo/bin:$PATH

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESKTOP_ROOT="${PLAIN_DESKTOP_ROOT:-$REPO_ROOT/../plain-desktop}"
PACKAGE="${PLAIN_PACKAGE:-com.ismartcoding.plain.debug}"

OUTPUT_DIR=""
while [ $# -gt 0 ]; do
  case "$1" in
    --output) OUTPUT_DIR="$2"; shift 2 ;;
    *) shift ;;
  esac
done

if [ -t 1 ]; then
  C_OK=$'\033[32m'; C_BAD=$'\033[31m'; C_WARN=$'\033[33m'; C_OFF=$'\033[0m'
else
  C_OK=""; C_BAD=""; C_WARN=""; C_OFF=""
fi

say() { printf '%s\n' "$*"; }
head1() { printf '\n=== %s ===\n' "$*"; }

# --- case accumulation -------------------------------------------------------
_CASES=""
FAILED=0
BLOCKED=0

_escape() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | tr '\n' ' '; }

case_pass() {
  say "  ${C_OK}PASS${C_OFF}  $1 ${2:+— $2}"
  _CASES="${_CASES}$(printf '{"name":"%s","status":"PASS","detail":"%s"},' "$(_escape "$1")" "$(_escape "$2")")"
}
case_fail() {
  say "  ${C_BAD}FAIL${C_OFF}  $1 ${2:+— $2}"
  FAILED=1
  _CASES="${_CASES}$(printf '{"name":"%s","status":"FAIL","detail":"%s"},' "$(_escape "$1")" "$(_escape "$2")")"
}
case_block() {
  say "  ${C_WARN}BLOCKED${C_OFF}  $1 ${2:+— $2}"
  BLOCKED=1
  _CASES="${_CASES}$(printf '{"name":"%s","status":"BLOCKED","detail":"%s"},' "$(_escape "$1")" "$(_escape "$2")")"
}

# Emit one case per JUnit XML suite directory, e.g. shared/build/test-results/testAndroidHostTest
count_junit_dir() {
  local dir="$1" tests=0 skipped=0 failures=0 errors=0 f line
  [ -d "$dir" ] || { printf '0 0 0 0'; return 1; }
  for f in "$dir"/TEST-*.xml; do
    [ -e "$f" ] || continue
    line="$(head -2 "$f" | tr '\n' ' ')"
    local t s fa e
    t=$(printf '%s' "$line" | sed -n 's/.* tests="\([0-9]*\)".*/\1/p')
    s=$(printf '%s' "$line" | sed -n 's/.* skipped="\([0-9]*\)".*/\1/p')
    fa=$(printf '%s' "$line" | sed -n 's/.* failures="\([0-9]*\)".*/\1/p')
    e=$(printf '%s' "$line" | sed -n 's/.* errors="\([0-9]*\)".*/\1/p')
    tests=$((tests + ${t:-0})); skipped=$((skipped + ${s:-0}))
    failures=$((failures + ${fa:-0})); errors=$((errors + ${e:-0}))
  done
  printf '%s %s %s %s' "$tests" "$skipped" "$failures" "$errors"
}

report_write() {
  local release=true
  # A blocked case means the gate was not fully covered. Reporting that as a
  # pass is how a release ships with an untested surface.
  [ "$FAILED" -eq 0 ] || release=false
  [ "$BLOCKED" -eq 0 ] || release=false
  local body="[${_CASES%,}]"
  if [ -n "$OUTPUT_DIR" ]; then
    mkdir -p "$OUTPUT_DIR"
    printf '{"cases":%s,"releaseReady":%s}\n' "$body" "$release" > "$OUTPUT_DIR/report.json"
  fi
  head1 "summary"
  say "  failures=$FAILED blocked=$BLOCKED"
  [ "$FAILED" -eq 0 ] || return 1
  return 0
}

# --- device helpers ----------------------------------------------------------
have_adb() { command -v adb >/dev/null 2>&1 && adb devices | grep -qw device; }

# Resolve a device serial by its stable adb `model:` field. Short transport ids
# rotate between reconnects, so never match on them.
serial_for_model() {
  local want="$1" serial rest
  adb devices -l | while read -r serial rest; do
    [ "$serial" = "List" ] && continue
    # `adb devices -l` prints "<serial>  device  ... model:<name> ..." — the
    # state word is the second column, so match the state there, not on serial.
    case "$rest" in device\ *) ;; *) continue ;; esac
    case "$rest" in *"model:${want}"*) printf '%s\n' "$serial" ;; esac
  done | head -1
}

# First non-loopback IPv4 across every interface — Pixel 7 comes up on wlan1 and
# Pixel 9 on wlan0, so the interface name is never assumed.
device_ip() {
  adb -s "$1" shell 'ip -4 addr' 2>/dev/null | tr -d '\r' \
    | grep -oE 'inet [0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' \
    | grep -v 'inet 127\.' | head -1 | awk '{print $2}'
}

# Bring a device to the state the tests need: awake, permissions granted, app
# running. Refuses to guess — an unresolved serial is an error, not a fallback.
prepare_device() {
  local dev="$1"
  if [ -z "$dev" ]; then say "!! no serial resolved"; return 1; fi
  adb -s "$dev" shell input keyevent KEYCODE_WAKEUP
  adb -s "$dev" shell svc power stayon true
  adb -s "$dev" shell wm dismiss-keyguard 2>/dev/null
  sleep 1
  local pkg="$PACKAGE"
  adb -s "$dev" shell appops set "$pkg" SYSTEM_ALERT_WINDOW allow 2>/dev/null
  adb -s "$dev" shell appops set "$pkg" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null
  adb -s "$dev" shell appops set "$pkg" READ_EXTERNAL_STORAGE allow 2>/dev/null
  adb -s "$dev" shell appops set "$pkg" WRITE_EXTERNAL_STORAGE allow 2>/dev/null
  adb -s "$dev" shell pm grant "$pkg" android.permission.POST_NOTIFICATIONS 2>/dev/null
  adb -s "$dev" shell am force-stop "$pkg"
  adb -s "$dev" shell am start -n "$pkg/com.ismartcoding.plain.MainActivity" >/dev/null 2>&1
  sleep 5
}

# Wait for the HTTP service to answer on the device; prints nothing on success.
wait_for_health() {
  local ip="$1" port="${2:-8080}" tries="${3:-20}" code
  while [ "$tries" -gt 0 ]; do
    code=$(curl -s -m 3 -o /dev/null -w '%{http_code}' "http://${ip}:${port}/health" 2>/dev/null)
    [ "$code" = "200" ] && return 0
    tries=$((tries - 1)); sleep 1
  done
  return 1
}