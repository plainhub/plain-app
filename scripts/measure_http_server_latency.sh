#!/usr/bin/env bash
# HTTP service start/stop latency metric (debug build + connected device).
#
# Guards the 2026-09 regressions: stop took 5s+ (engine teardown self-joined
# inside the /shutdown route handler) and the first start took 2s+ (cold
# GraphQL schema / PKCS12 keystore parse on the tap path). The app logs
# lifecycle phase timings with tag PlainApp; this script drives start/stop via
# the token-protected ADB broadcasts, extracts the timings from logcat and
# fails when a threshold is exceeded.
#
# Usage:
#   scripts/measure_http_server_latency.sh [adb-serial]
# Env overrides: PACKAGE, START_MS_MAX (default 1500), STOP_MS_MAX (default 1500)
#
# Requires: adb, python3, the githubDebug (or any debug) build installed.

set -euo pipefail

SERIAL="${1:-}"
PACKAGE="${PACKAGE:-com.ismartcoding.plain.debug}"
START_MS_MAX="${START_MS_MAX:-1500}"
STOP_MS_MAX="${STOP_MS_MAX:-1500}"

ADB=(adb)
[ -n "$SERIAL" ] && ADB=(adb -s "$SERIAL")

fail() { echo "FAIL: $*" >&2; exit 1; }

"${ADB[@]}" get-state >/dev/null 2>&1 || fail "no device (pass serial as \$1)"
"${ADB[@]}" shell "pm path $PACKAGE" >/dev/null 2>&1 || fail "$PACKAGE not installed"

echo "== cold start app, wait for warm-up =="
"${ADB[@]}" shell am force-stop "$PACKAGE"
sleep 1
"${ADB[@]}" logcat -c
"${ADB[@]}" shell am start -n "$PACKAGE/com.ismartcoding.plain.MainActivity" >/dev/null
sleep 8

# The automation token is re-randomized on every app launch; read it from the
# debug app's datastore (run-as works on debug builds only).
TOKEN=$("${ADB[@]}" shell "run-as $PACKAGE cat files/datastore/settings.preferences_pb" | python3 -c '
import sys, re
data = sys.stdin.buffer.read().replace(b"\r", b"")
i = data.find(b"adb_token")
m = re.search(rb"[A-Za-z0-9]{32}", data[i + len(b"adb_token"):i + 80]) if i >= 0 else None
print(m.group(0).decode() if m else "")')
[ -n "$TOKEN" ] || fail "could not read adb_token from $PACKAGE datastore (open the app once, then retry)"

# Ensure a known OFF state (idempotent).
"${ADB[@]}" shell am broadcast -a "$PACKAGE.action.STOP_HTTP_SERVER" -p "$PACKAGE" --es token "$TOKEN" >/dev/null
sleep 3

wait_for_log() { # $1: grep pattern, $2: timeout seconds
    local pattern="$1" timeout="$2" waited=0
    while [ "$waited" -lt "$((timeout * 10))" ]; do
        local line
        line="$("${ADB[@]}" logcat -d -s PlainApp 2>/dev/null | grep "$pattern" | tail -1)"
        [ -n "$line" ] && { echo "$line"; return 0; }
        sleep 0.1
        waited=$((waited + 1))
    done
    return 1
}

extract_ms() { # $1: line, $2: label before the number, e.g. "total"
    echo "$1" | grep -oE "$2 [0-9]+ms" | grep -oE '[0-9]+' | tail -1
}

RESULT=0

measure_stop() {
    echo "== measure stop =="
    "${ADB[@]}" logcat -c
    "${ADB[@]}" shell am broadcast -a "$PACKAGE.action.STOP_HTTP_SERVER" -p "$PACKAGE" --es token "$TOKEN" >/dev/null
    local line ms
    line="$(wait_for_log "stopHttpServerCore total" 30)" || fail "stop never completed (no 'stopHttpServerCore total' log)"
    ms="$(extract_ms "$line" "total")"
    echo "   stop core: ${ms}ms (threshold ${STOP_MS_MAX}ms)"
    [ "$ms" -le "$STOP_MS_MAX" ] || RESULT=1
}

measure_start() { # $1: label
    echo "== measure start ($1) =="
    "${ADB[@]}" logcat -c
    "${ADB[@]}" shell am broadcast -a "$PACKAGE.action.START_HTTP_SERVER" -p "$PACKAGE" --es token "$TOKEN" >/dev/null
    local line ms
    line="$(wait_for_log "HTTP server started on port" 30)" || fail "start never completed (no 'HTTP server started' log)"
    ms="$(extract_ms "$line" "total")"
    echo "   start core ($1): ${ms}ms (threshold ${START_MS_MAX}ms)"
    [ "$ms" -le "$START_MS_MAX" ] || RESULT=1
}

measure_start "first after warm-up"
measure_stop
measure_start "warm"

if [ "$RESULT" -ne 0 ]; then
    echo "RESULT: FAIL — latency threshold exceeded (start>${START_MS_MAX}ms or stop>${STOP_MS_MAX}ms)"
    exit 1
fi
echo "RESULT: PASS"
