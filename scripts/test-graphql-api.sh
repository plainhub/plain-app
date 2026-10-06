#!/bin/bash
# GraphQL query surface, run against a phone's main listener the same way the
# web client talks to it: log in over the WebSocket `auth=1` handshake, then
# send xchacha-encrypted token-mode queries. `gql-client.mjs` speaks the
# protocol; this script owns the query list and the pass/fail accounting.
#
#   ./scripts/test-graphql-api.sh <base-url> [client-id]
#
# 2FA is on by default (`ws_login.rs`: `auth_two_factor` defaults to true), so
# the first login comes back PENDING and the phone shows "Allow Desktop
# Access". Nothing here can tap that dialog — the caller passes a command to
# run at that moment in PLAIN_APPROVE_CMD, e.g. `approve_desktop_access <serial>`.
set -uo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
CLIENT_ID="${2:-plain-api-test}"
CLIENT="$(dirname "${BASH_SOURCE[0]}")/gql-client.mjs"
QFILE="$(mktemp)"
OUT="$(mktemp)"
trap 'rm -f "$QFILE" "$OUT"' EXIT

if ! command -v node >/dev/null 2>&1; then
  echo "node is required (scripts/gql-client.mjs)" >&2
  exit 1
fi

emit() { printf '%s\t%s\n' "$1" "$2" >>"$QFILE"; }

# Roots behind an Android permission the phone may not hold: `public_gate`
# answers `no_permission` (or a feature-specific variant) instead of data.
# Either answer is correct — data once granted, a refusal while not — so they
# are scored on refusing *consistently*, not on returning rows.
emit_gated() { printf '@%s\t%s\n' "$1" "$2" >>"$QFILE"; }

# --- app, device, peers ------------------------------------------------------
emit app '{ app { clientId deviceName deviceType buildChannel developerMode debug downloadsDir httpPort httpsPort } }'
emit device_info '{ deviceInfo { name manufacturer model osName osVersion appVersion appBuildNumber } }'
emit device_status '{ deviceStatus { uptimeSec batteryLevel charging storageAvailable } }'
emit peers '{ peers { id name ip port online } }'
emit mounts '{ mounts { id name path mountPoint totalBytes freeBytes } }'
emit path_exists '{ pathExists(path: "/sdcard") }'

# --- notes, feeds, bookmarks, tags ------------------------------------------
emit notes '{ notes(offset: 0, limit: 5, query: "") { id title deletedAt createdAt } }'
emit feeds '{ feeds { id name url lastSyncAt } }'
emit feed_entries '{ feedEntries(offset: 0, limit: 5, query: "") { feedId id title url } }'
emit feed_sync_states '{ feedSyncStates { feedId status error } }'
emit bookmarks '{ bookmarks { id url title groupId pinned } }'
emit bookmark_groups '{ bookmarkGroups { id name itemCount } }'
emit tags '{ tags(type: DEFAULT) { id name count } }'

# --- media (READ_MEDIA_*/WRITE_EXTERNAL_STORAGE) ----------------------------
emit_gated images '{ images(offset: 0, limit: 5, query: "", sortBy: DATE_DESC) { id title path size } }'
emit_gated videos '{ videos(offset: 0, limit: 5, query: "", sortBy: DATE_DESC) { id title path size durationMs } }'
emit_gated audios '{ audios(offset: 0, limit: 5, query: "", sortBy: DATE_DESC) { id title artist durationMs } }'
emit_gated docs '{ docs(offset: 0, limit: 5, query: "", sortBy: DATE_DESC) { id title path extension size } }'
emit doc_ext_groups '{ docExtGroups { ext count } }'
emit_gated files '{ files(root: "", offset: 0, limit: 5, query: "", sortBy: NAME_ASC) { mediaId name path size isDir } }'
emit_gated recent_files '{ recentFiles { mediaId name path size } }'
emit favorite_folders '{ favoriteFolders { rootPath fullPath alias } }'

# --- contacts, calls, sms ----------------------------------------------------
emit_gated contacts '{ contacts(offset: 0, limit: 5, query: "") { id firstName lastName nickname } }'
emit_gated calls '{ calls(offset: 0, limit: 5, query: "") { id number name durationSec } }'
emit sims '{ sims { id label number } }'
emit sms '{ sms(offset: 0, limit: 5, query: "") { id body address read } }'
emit sms_box_counts '{ smsBoxCounts { total inbox sent drafts } }'

# --- chat, clipboard, packages (QUERY_ALL_PACKAGES), notifications ---------
emit chat_channels '{ chatChannels { id name ownerId version } }'
emit_gated clipboard_items '{ clipboardItems(offset: 0, limit: 5, query: "") { id text source sensitive } }'
emit_gated packages '{ packages(offset: 0, limit: 5, query: "", sortBy: NAME_ASC) { id name version } }'
emit_gated notifications '{ notifications(offset: 0, limit: 5, query: "") { id appName title postedAt } }'

# --- pomodoro, image editor, database, prefs, logs --------------------------
emit pomodoro_today '{ pomodoroToday { date completedCount isRunning } }'
emit pomodoro_settings '{ pomodoroSettings { workDurationMin shortBreakDurationMin } }'
emit image_editor_projects '{ imageEditorProjects { id canvasWidth layerCount } }'
emit db_tables '{ dbTables }'
emit db_table_info '{ dbTableInfo(table: "sessions") { idKey } }'
emit user_prefs '{ userPrefs }'
emit system_prefs '{ systemPrefs }'
emit app_logs '{ appLogs(offset: 0, limit: 5, query: "") }'

PASS=0
FAIL=0
ERRORS=""

echo "=== GraphQL API Test ==="
echo "URL: ${BASE_URL}/graphql"
echo "Client ID: ${CLIENT_ID}"
echo ""

if node "$CLIENT" --host "$(printf '%s' "$BASE_URL" | sed -e 's#^[a-z]*://##' -e 's#:.*##')" \
  --port "$(printf '%s' "$BASE_URL" | sed -n 's#.*:\([0-9]*\)/.*#\1#p')" \
  --client-id "$CLIENT_ID" --queries "$QFILE" ${PLAIN_APPROVE_CMD:+--on-pending "$PLAIN_APPROVE_CMD"} >"$OUT"; then
  :
else
  echo "  FAIL [login] — could not complete the login handshake (see the log above)"
  echo ""
  echo "--- Summary ---"
  echo "Passed: 0"
  echo "Failed: 1"
  echo "Failures:\n  FAIL [login]: login handshake did not complete"
  exit 1
fi

while IFS=$'\t' read -r name status detail; do
  [ -z "$name" ] && continue
  case "$name" in
    @*)
      name="${name#@}"
      if [ "$status" = "ok" ]; then
        PASS=$((PASS + 1))
        echo "  PASS [$name] — $detail (permission granted)"
      elif printf '%s' "$detail" | grep -qE 'no_permission|_disabled'; then
        PASS=$((PASS + 1))
        echo "  PASS [$name] — refused: ${detail#GraphQL errors: }"
      else
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$name]: ${detail}"
        echo "  FAIL [$name] — ${detail}"
      fi
      ;;
    *)
      if [ "$status" = "ok" ]; then
        PASS=$((PASS + 1))
        echo "  PASS [$name] — $detail"
      else
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$name]: ${detail}"
        echo "  FAIL [$name] — ${detail}"
      fi
      ;;
  esac
done <"$OUT"

echo ""
echo "--- Summary ---"
echo "Passed: $PASS"
echo "Failed: $FAIL"
if [ -n "$ERRORS" ]; then
  printf "$ERRORS\n"
fi
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "All GraphQL API tests passed!"
  exit 0
else
  echo "Some tests failed. Check the output above."
  exit 1
fi