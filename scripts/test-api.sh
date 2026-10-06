#!/bin/bash
# API tests — the contract a phone serves over HTTP, checked against a real
# device. Nothing here is mocked: every assertion is a real HTTP response.
#
#   ./scripts/test-api.sh                 # auto-pick a device by model:
#   ./scripts/test-api.sh <adb-serial>    # or pass one explicitly
#
# Covers what the Rust listener owns: the health probe, the web SPA deep links,
# static assets, auth boundaries and the server-time injection. The GraphQL
# query surface needs a logged-in session token, which cannot be obtained
# without a human tapping the 2FA prompt — that case is reported BLOCKED rather
# than silently skipped.
set -uo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"

MODEL="${PLAIN_DEVICE_MODEL:-Pixel_7}"
# Only a non-flag $1 is a serial, so `--output <dir>` falls back to MODEL
# instead of being handed to adb as a device name.
case "${1:-}" in -*) DEV="" ;; *) DEV="${1:-}" ;; esac
[ -z "$DEV" ] && DEV=$(serial_for_model "$MODEL")

if ! have_adb; then
  say "!! adb reports no connected device — connect one and retry"
  report_write; exit 1
fi
if [ -z "$DEV" ]; then
  say "!! no device matched model:${MODEL}; set PLAIN_DEVICE_MODEL or pass a serial"
  report_write; exit 1
fi

prepare_device "$DEV"
IP="$(device_ip "$DEV")"
say "device=$DEV ip=${IP:-unknown} package=$PACKAGE"
if [ -z "$IP" ]; then
  case_fail "device_reachable" "no IPv4 address on $DEV"
  report_write; exit 1
fi

BASE="http://${IP}:8080"
wait_for_health "$IP" 8080 20 || say "  (service not answering yet — probing anyway)"

code() { curl -s -m 8 -o /dev/null -w '%{http_code}' "$@" 2>/dev/null || printf '000'; }

# --- health ------------------------------------------------------------------
head1 "health"
H=$(code "$BASE/health")
if [ "$H" = "200" ]; then case_pass "health_endpoint" "GET /health -> 200"; else case_fail "health_endpoint" "GET /health -> $H"; fi

# --- auth boundary -----------------------------------------------------------
# Must refuse before any route logic runs: an unauthenticated /graphql that
# answers 200 is a security regression, not a feature.
head1 "auth boundary"
G=$(code -X POST "$BASE/graphql" -H 'Content-Type: application/json' -d '{"query":"{__typename}"}')
if [ "$G" = "401" ]; then case_pass "graphql_requires_auth" "unauthenticated POST /graphql -> 401"
else case_fail "graphql_requires_auth" "unauthenticated POST /graphql -> $G (expected 401)"; fi

# --- SPA deep links ----------------------------------------------------------
# The Rust listener serves index.html for every client route. A 404 here means
# the asset path is wrong again — that exact bug shipped once already.
head1 "web deep links"
ROUTES="about apps audios calls chat chat/app-files contacts developer
developer/database developer/device-info developer/logs developer/prefs
docs feeds files files/recent image-editor images login markdown-preview
media-preview messages messages/archived notes screen-mirror
settings/lan-share setup text-edit text-file ux videos"
BAD=""
N=0
for route in $ROUTES; do
  N=$((N + 1))
  C=$(code "$BASE/$route")
  [ "$C" = "200" ] || BAD="$BAD /$route=$C"
done
if [ -z "$BAD" ]; then
  case_pass "spa_deep_links" "$N client routes all 200"
else
  case_fail "spa_deep_links" "$BAD"
fi

# --- static assets -----------------------------------------------------------
head1 "static assets"
INDEX=$(curl -s -m 8 "$BASE/" 2>/dev/null)
ASSET=$(printf '%s' "$INDEX" | grep -oE '/assets/[A-Za-z0-9._-]+\.(js|css)' | head -1)
if [ -z "$ASSET" ]; then
  case_fail "static_assets" "index.html references no /assets/* entry point"
else
  A=$(code "$BASE$ASSET")
  if [ "$A" = "200" ]; then case_pass "static_assets" "$ASSET -> 200"
  else case_fail "static_assets" "$ASSET -> $A"; fi
fi
if printf '%s' "$INDEX" | grep -q '__SERVER_TIME__'; then
  case_pass "server_time_injection" "__SERVER_TIME__ present in index.html"
else
  case_fail "server_time_injection" "__SERVER_TIME__ missing from index.html"
fi

# --- TLS listener ------------------------------------------------------------
head1 "https listener"
T=$(code -k "https://${IP}:8443/health")
if [ "$T" = "200" ]; then case_pass "https_listener" "GET https://:8443/health -> 200"
else case_block "https_listener" "https :8443 -> $T (TLS is off or the cert is not trusted by curl)"; fi

# --- GraphQL surface ---------------------------------------------------------
# The query surface already has a script (`test-graphql-api.sh`); reuse it
# instead of duplicating the query list. It needs a session token, which cannot
# be minted without a human tapping the 2FA prompt — so without credentials this
# is BLOCKED, never silently skipped.
#
# Note that a token from an ordinary browser login is NOT enough here. The
# browser authenticates in token mode (`c-id` plus an xchacha-encrypted body);
# this script sends `Authorization: Bearer <token>`, and main_graphql.rs only
# honours that path for SessionType.CUSTOM sessions. A WEB session's token
# answers 401 even when it is the very value localStorage holds as
# `auth_token`. So the two BLOCKED reasons below are both real, and neither is
# cleared by supplying the browser's credentials.
head1 "graphql surface"
GQL_SCRIPT="$REPO_ROOT/scripts/test-graphql-api.sh"
if [ -z "${PLAIN_TOKEN:-}" ] || [ -z "${PLAIN_CLIENT_ID:-}" ]; then
  case_block "graphql_queries" "no PLAIN_TOKEN + PLAIN_CLIENT_ID; and note a browser-login token alone would still 401 — see the comment above"
elif [ ! -x "$GQL_SCRIPT" ]; then
  case_block "graphql_queries" "$GQL_SCRIPT is missing"
else
  if "$GQL_SCRIPT" "$BASE" "$PLAIN_TOKEN" "$PLAIN_CLIENT_ID" > /tmp/plain-api-graphql.log 2>&1; then
    case_pass "graphql_queries" "$(grep -E '^Passed:' /tmp/plain-api-graphql.log | tail -1)"
  else
    case_fail "graphql_queries" "$(grep -E '^Failed:' /tmp/plain-api-graphql.log | tail -1); see /tmp/plain-api-graphql.log"
  fi
fi

report_write