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
#
# The base url must name the build under test. `verify_app_under_test` refuses
# to let a request out until the package answering on that port is the one this
# checkout produced: a release build and a debug build sit on the same phone,
# each holding its own port, and a probe aimed at the wrong one answers
# perfectly normally. The caller passes the device as $PLAIN_SERIAL.
set -uo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"

# No default port: the caller must name the build under test explicitly.
BASE_URL="${1:?usage: %s <base-url> [client-id]  (port is the one the build under test bound)}"
CLIENT_ID="${2:-plain-api-test}"
CLIENT="$(dirname "${BASH_SOURCE[0]}")/gql-client.mjs"
GQL_PORT="$(printf '%s' "$BASE_URL" | sed -n 's#.*:\([0-9][0-9]*\)\(/\{0,1\}\).*#\1#p')"

if [ -z "${PLAIN_SERIAL:-}" ]; then
  echo "!! PLAIN_SERIAL is not set — refusing to measure an unidentified device" >&2
  echo "   the release and the debug build answer on different ports, so an" >&2
  echo "   unverified target silently returns results from the wrong app" >&2
  exit 1
fi
if ! verify_app_under_test "$PLAIN_SERIAL" "$GQL_PORT"; then
  echo "" >&2
  echo "--- Summary ---" >&2
  echo "Passed: 0" >&2
  echo "Failed: 1" >&2
  echo "Failures:" >&2
  echo "  FAIL [app_under_test]: the device is not running the build this checkout produced" >&2
  exit 1
fi

QFILE="$(mktemp)"
OUT="$(mktemp)"
ERR="$(mktemp)"
trap 'rm -f "$QFILE" "$OUT" "$ERR"' EXIT

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
# A search box holds free text, so `SearchHelper.buildTextFilter` escapes it into
# one `text:` token before the query is built. The `\\` below is a GraphQL
# escape, so the phone receives the single backslash its tokenizer needs —
# without it the term arrives as the field `Meeting` and the search fails
# instead of matching. Paired with the `!` case below: escaping must not have
# weakened the refusal.
emit notes_freetext_with_colon '{ notes(offset: 0, limit: 1, query: "text:Meeting:\\ notes trash:false") { id } }'
emit '!notes_unknown_field_refused=zzz_no_such_field' '{ notes(offset: 0, limit: 1, query: "zzz_no_such_field:x trash:false") { id } }'
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
# `/sdcard`, not "": the resolver rejects a relative root, and while the
# permission gate was short-circuiting this case the rejection was scored as the
# expected "no permission" answer — a wrong argument passing as a granted one.
emit_gated files '{ files(root: "/sdcard", offset: 0, limit: 5, query: "", sortBy: NAME_ASC) { mediaId name path size isDir } }'
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

# The served schema itself, so the contract snapshot can be put next to what
# the server actually serves. No host test can do this: the schema only exists
# inside the running Rust server. `raw:` makes gql-client return the decrypted
# body; the case is scored by score_contract_types, never on "did it answer".
emit 'raw:contract_types' '{ __schema { types { name kind } } }'

# Bidirectional set comparison. A type the contract declares but the server
# does not serve is dead documentation; one the server serves but the contract
# omits means the App-side view of the schema is incomplete. ApiContractTest
# can see neither — it only checks the snapshot against its own conventions —
# which is how `interface MediaItem` and two platform enums sat in the
# contract for weeks without anything going red.
score_contract_types() {
  local contract served declared only_served only_declared
  contract="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/shared/apitest/schema.graphqls"
  if [ -z "${RAW_TYPES:-}" ]; then
    FAIL=$((FAIL + 1))
    ERRORS="${ERRORS}\n  FAIL [contract_types]: introspection returned no payload"
    echo "  FAIL [contract_types] — introspection returned no payload"
    return
  fi
  served=$(printf '%s' "$RAW_TYPES" | grep -o '"name":"[^"]*"' | sed 's/^"name":"//; s/"$//' \
    | grep -vE '^__|^(String|Int|Float|Boolean|ID)$' | sort -u)
  declared=$(grep -oE '^(type|interface|enum|union|input|scalar) [A-Za-z_][A-Za-z0-9_]*' "$contract" \
    | awk '{print $2}' | grep -v '^__' | sort -u)
  if [ -z "$served" ] || [ -z "$declared" ]; then
    FAIL=$((FAIL + 1))
    ERRORS="${ERRORS}\n  FAIL [contract_types]: served type list ($(printf '%s' "$served" | wc -l | tr -d ' ') parsed) or contract type list ($(printf '%s' "$declared" | wc -l | tr -d ' ') parsed) came out empty"
    echo "  FAIL [contract_types] — a type list came out empty, nothing was compared"
    return
  fi
  only_served=$(comm -23 <(printf '%s\n' "$served") <(printf '%s\n' "$declared"))
  only_declared=$(comm -13 <(printf '%s\n' "$served") <(printf '%s\n' "$declared"))
  if [ -n "$only_served" ] || [ -n "$only_declared" ]; then
    FAIL=$((FAIL + 1))
    ERRORS="${ERRORS}\n  FAIL [contract_types]: schema.graphqls drifted from the served schema — served only: $(echo "${only_served:-none}" | tr '\n' ' ')| declared only: $(echo "${only_declared:-none}" | tr '\n' ' ')"
    echo "  FAIL [contract_types] — contract drifted from the served schema:"
    [ -n "$only_served" ] && echo "      served but undeclared: $(echo "$only_served" | tr '\n' ' ')"
    [ -n "$only_declared" ] && echo "      declared but not served: $(echo "$only_declared" | tr '\n' ' ')"
    return
  fi
  PASS=$((PASS + 1))
  echo "  PASS [contract_types] — $(printf '%s\n' "$served" | wc -l | tr -d ' ') served types, all declared in schema.graphqls"
}

PASS=0
FAIL=0
ERRORS=""
RAW_TYPES=""

echo "=== GraphQL API Test ==="
echo "URL: ${BASE_URL}/graphql"
echo "Client ID: ${CLIENT_ID}"
echo ""

# The client's stderr carries the actual reason a login stalled (wrong
# password, rate limited, prompt never accepted) — kept beside the results, and
# printed when one fails. Kept out of $OUT on purpose: it is progress chatter,
# not `name<TAB>status<TAB>detail` rows, and mixing them invents fake failures.
if node "$CLIENT" --host "$(printf '%s' "$BASE_URL" | sed -e 's#^[a-z]*://##' -e 's#:.*##')" \
  --port "$(printf '%s' "$BASE_URL" | sed -n 's#.*:\([0-9][0-9]*\)\(/\{0,1\}\).*#\1#p')" \
  --client-id "$CLIENT_ID" --queries "$QFILE" ${PLAIN_APPROVE_CMD:+--on-pending "$PLAIN_APPROVE_CMD"} \
  >"$OUT" 2>"$ERR"; then
  :
else
  echo ""
  echo "--- login log ---"
  cat "$ERR"
  echo ""
  echo "--- Summary ---"
  echo "Passed: 0"
  echo "Failed: 1"
  echo "Failures:"
  echo "  FAIL [login]: login handshake did not complete"
  exit 1
fi

while IFS=$'\t' read -r name status detail; do
  [ -z "$name" ] && continue
  case "$name" in
    raw:contract_types)
      # Not scored here — the payload only means something once it has been
      # compared against the contract snapshot.
      RAW_TYPES="$detail"
      ;;
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
    !*)
      # Must be refused, and the refusal has to name the offending field — a
      # query that silently matched everything is the hazard this guards. The
      # field to look for rides along in the case name after `=`, since the
      # wire format has no column to spare.
      ref="${name#!}"
      expected="${ref#*=}"
      name="${ref%%=*}"
      if [ "$status" = "ok" ]; then
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$name]: the query was accepted; it should have been refused by name"
        echo "  FAIL [$name] — accepted, but an unknown field must be refused"
      elif [ -n "$expected" ] && printf '%s' "$detail" | grep -qF "$expected"; then
        PASS=$((PASS + 1))
        echo "  PASS [$name] — refused: ${detail#GraphQL errors: }"
      else
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$name]: refused, but not by name: ${detail}"
        echo "  FAIL [$name] — refused without naming the field: ${detail}"
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

score_contract_types

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