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

# The package under test, derived from the build rather than written down here:
# `applicationId` plus the `debug` flavor's `applicationIdSuffix`. A hardcoded
# name is how a debug build ends up installed while the release build gets
# measured — the two coexist on a phone, they answer on different ports, and
# nothing about the result looks wrong.
plain_app_package() {
  local gradle="$REPO_ROOT/app/build.gradle.kts" base suffix
  base="$(sed -n 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$gradle" | head -1)"
  # `applicationIdSuffix` sits in `buildTypes { debug { … } }`, not in
  # `productFlavors`, so the block is located by name rather than by position.
  suffix="$(awk '/buildTypes[[:space:]]*\{/{f=1} f && /applicationIdSuffix/{print; exit}' "$gradle" \
    | sed -n 's/.*"\([^"]*\)".*/\1/p')"
  [ -n "$base" ] || { say "!! could not read applicationId from $gradle"; return 1; }
  printf '%s%s\n' "$base" "$suffix"
}

PLAIN_APP_ID="$(sed -n 's/^[[:space:]]*applicationId[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$REPO_ROOT/app/build.gradle.kts" | head -1)"
PACKAGE="${PLAIN_PACKAGE:-$(plain_app_package)}"

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

# Refuse to measure anything until the package on the phone is provably the one
# this checkout produced. Both the release and the debug build are installed on
# a dev phone, they listen on different ports, and the release one keeps running
# untouched — so a probe aimed at the wrong package returns plausible numbers
# and every later conclusion is built on them. Three things have to hold:
#
#   * the package exists (otherwise there is nothing to measure);
#   * it was installed at or after the APK was built (otherwise it is an older
#     build, whatever its version string says);
#   * the process serving the port under test is that package's.
#
# `verify_app_under_test` prints what it found and returns non-zero otherwise;
# every script calls it before its first request.
# `package: name='…' versionCode='…' versionName='…'` for a built APK, via the
# SDK's aapt2. Returns non-zero when no build-tools aapt2 can be found.
apk_badging() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}" aapt
  aapt="$(ls -t "$sdk"/build-tools/*/aapt2 2>/dev/null | head -1)"
  [ -n "$aapt" ] || return 1
  "$aapt" dump badging "$1" 2>/dev/null | sed -n "s/^package: name='\([^']*\)' versionCode='\([^']*\)' versionName='\([^']*\)'.*/\1 \2 \3/p" | head -1
}

# Refuse to measure anything until the package on the phone is provably the one
# this checkout produced. Both the release and the debug build are installed on
# a dev phone, they listen on different ports, and the release one keeps running
# untouched — so a probe aimed at the wrong package returns plausible numbers
# and every later conclusion is built on them. Three things have to hold:
#
#   * the APK on disk really is the package under test (aapt2, not a guess);
#   * the phone runs that exact versionCode — compared by code, not by mtime,
#     because a `touch` or a rebuild of an unrelated flavor would otherwise make
#     a perfectly good install look stale;
#   * the release build is not also running, since it answers on its own port.
verify_app_under_test() {
  local dev="$1" port="${2:-}" installed="" running="" apk="" badging="" apk_pkg="" apk_code="" dev_code=""

  if [ -z "$dev" ]; then say "!! no serial resolved"; return 1; fi

  installed="$(adb -s "$dev" shell "pidof $PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  if [ -z "$installed" ]; then
    if adb -s "$dev" shell "pm path $PACKAGE" 2>/dev/null | grep -q '^package:'; then
      say "!! $PACKAGE is installed on $dev but not running"
      say "   start it: source scripts/test-lib.sh && prepare_device $dev"
    else
      say "!! $PACKAGE is not installed on $dev"
      say "   build and install it: ./gradlew :app:assembleGoogleDebug && adb -s $dev install -r <apk>"
    fi
    return 1
  fi

  apk="$(ls -t "$REPO_ROOT"/app/build/outputs/apk/*/debug/*.apk 2>/dev/null | head -1)"
  if [ -z "$apk" ]; then
    say "!! no debug APK under app/build/outputs/apk — nothing to compare the phone against"
    return 1
  fi
  if ! badging="$(apk_badging "$apk")" || [ -z "$badging" ]; then
    say "!! could not read $apk with aapt2 — refusing to guess whether it is the installed one"
    return 1
  fi
  apk_pkg="$(printf '%s' "$badging" | awk '{print $1}')"
  apk_code="$(printf '%s' "$badging" | awk '{print $2}')"
  if [ "$apk_pkg" != "$PACKAGE" ]; then
    say "!! $apk is $apk_pkg, not $PACKAGE"
    say "   this script tests the debug build; install that one instead"
    return 1
  fi

  dev_code="$(adb -s "$dev" shell "dumpsys package $PACKAGE" 2>/dev/null \
    | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -1 | tr -d '\r')"
  if [ -z "$dev_code" ] || [ "$dev_code" != "$apk_code" ]; then
    say "!! $PACKAGE on $dev is versionCode ${dev_code:-unknown}, the APK is $apk_code"
    say "   apk: $apk"
    say "   reinstall it: adb -s $dev install -r $apk"
    return 1
  fi

  # `${2+x}` rather than `-n`: a caller that passed an empty port (a base url
  # that did not parse) must be refused, while a caller that deliberately left
  # the port out — the UI suite, which never dials it — is fine. `-n` treated
  # both as "no port" and skipped the check that was supposed to catch it.
  if [ -n "${2+x}" ] && [ -z "$port" ]; then
    say "!! no port to verify — the base url did not yield one"
    say "   expected a url like http://<ip>:<port>, the port being the one $PACKAGE bound"
    return 1
  fi

  if [ -n "$port" ]; then
    # The port under test has to be one this build bound. `netstat -ltnp` shows
    # no pid on Android, so a listening socket cannot be traced back to a
    # process; the build's own startup line can. Without this, a caller aiming
    # at the other build's port is caught only while that build happens to run.
    local own_http own_https
    if ! split_app_ports "$dev" own_http own_https; then
      say "!! could not read the ports $PACKAGE bound — refusing to assume port $port is ours"
      return 1
    fi
    if [ "$port" != "$own_http" ] && [ "$port" != "$own_https" ]; then
      say "!! port $port does not belong to $PACKAGE: this build bound $own_http/$own_https"
      say "   $PLAIN_APP_ID keeps its own port on the same phone"
      say "   point the script at http://<device-ip>:$own_http"
      return 1
    fi
  fi

  if [ -n "$port" ] && [ -n "$PLAIN_APP_ID" ] && [ "$PLAIN_APP_ID" != "$PACKAGE" ]; then
    local other
    other="$(adb -s "$dev" shell "pidof $PLAIN_APP_ID" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
    if [ -n "$other" ]; then
      say "!! both builds are running: $PACKAGE (pid $installed) and $PLAIN_APP_ID (pid $other)"
      say "   they answer on different ports and a request to the wrong one looks completely normal"
      say "   stop the other one: adb -s $dev shell am force-stop $PLAIN_APP_ID"
      return 1
    fi
  fi

  say "app under test: $PACKAGE $apk_code (pid $installed)"
  return 0
}

# The ports this build actually bound. They are a stored preference, not a
# constant: a phone that once ran the release build keeps that build's port, so
# a hardcoded 8080 silently measures whichever app answers there. Filtered by
# pid so that, with both builds running, the line belongs to the one asked for.
app_ports() {
  local dev="$1" pid line
  pid="$(adb -s "$dev" shell "pidof $PACKAGE" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  if [ -z "$pid" ]; then say "!! $PACKAGE is not running on $dev"; return 1; fi
  line="$(adb -s "$dev" logcat -d --pid="$pid" 2>/dev/null \
    | grep -F 'HTTP server started on ports' | tail -1 | tr -d '\r')"
  if [ -z "$line" ]; then
    say "!! no 'HTTP server started on ports' line in $PACKAGE's log (pid $pid)"
    return 1
  fi
  # The trailing newline matters: `read` reports failure on input that ends
  # without one, so `printf '%s'` made every caller treat a successful lookup
  # as a missing one.
  printf '%s\n' "$line" | sed -n 's/.*on ports \([0-9]*\)\/\([0-9]*\).*/\1 \2/p'
}

# Splits `app_ports` output into the two variables named by `http_var` and
# `https_var`. Command substitution rather than `< <(…)`: the approval command
# the API client runs is a POSIX `sh` child, where process substitution is a
# syntax error — which reads as "the gate failed" rather than "the gate broke".
split_app_ports() {
  local dev="$1" http_var="$2" https_var="$3" out
  if ! out="$(app_ports "$dev")" || [ -z "$out" ]; then return 1; fi
  printf -v "$http_var" '%s' "${out%% *}"
  printf -v "$https_var" '%s' "${out##* }"
  return 0
}

# Accept the phone's "Allow Desktop Access" prompt. The Rust login answers
# PENDING and issues no token until a human taps it (`auth_two_factor`
# defaults to true in ws_login.rs), so a shell-driven login is only possible
# if something taps for it. Finds the button by its label instead of a fixed
# coordinate, and polls because the prompt lands a beat after the request.
approve_desktop_access() {
  local dev="$1" i dump bounds coords
  for i in $(seq 1 20); do
    adb -s "$dev" shell uiautomator dump /sdcard/plain-approve.xml >/dev/null 2>&1
    dump=$(adb -s "$dev" shell cat /sdcard/plain-approve.xml 2>/dev/null | tr -d '\r')
    # Four separate numbers, never one joined one: stripping the brackets off
    # "1562][601" merges two coordinates into "1562601", which then reads as
    # three numbers instead of four and silently skips the tap.
    bounds=$(printf '%s' "$dump" | grep -o 'text="Allow"[^>]*bounds="[^"]*"' \
      | grep -o 'bounds="[^"]*"' | head -1 | grep -oE '[0-9]+' | tr '\n' ' ')
    coords=$(printf '%s' "$bounds" | awk '{ if (NF == 4) print int(($1 + $3) / 2), int(($2 + $4) / 2) }')
    if [ -n "$coords" ]; then
      set -- $coords
      adb -s "$dev" shell input tap "$1" "$2"
      return 0
    fi
    sleep 2
  done
  # Say what was actually on screen: "no prompt" is the single most useless
  # failure text to hand back, and it is the one this keeps hitting.
  echo "no 'Allow Desktop Access' prompt after 40s; on screen:" >&2
  printf '%s' "$dump" | tr '>' '\n' | grep -o 'text="[^"]\+"' | head -8 >&2
  return 1
}

# Wait for the HTTP service to answer on the device; prints nothing on success.
wait_for_health() {
  local ip="$1" port="${2:?wait_for_health needs the port the build under test bound}" tries="${3:-20}" code
  while [ "$tries" -gt 0 ]; do
    code=$(curl -s -m 3 -o /dev/null -w '%{http_code}' "http://${ip}:${port}/health" 2>/dev/null)
    [ "$code" = "200" ] && return 0
    tries=$((tries - 1)); sleep 1
  done
  return 1
}