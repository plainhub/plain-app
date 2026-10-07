#!/bin/bash
# Mutation surface, run against a phone's main listener the same way the web
# client talks to it. Companion to test-graphql-api.sh, which only reads.
#
#   ./scripts/test-graphql-mutations.sh <base-url> [client-id]
#
# Every mutation here is proven to touch nothing before it is sent, and the
# proof is not "I read the code":
#
#   1. The selector is a field the table actually supports (`ids:`), carrying a
#      value that cannot exist. `id:` is NOT supported — it used to be dropped,
#      which emptied the clause list and rendered it as `1=1`, turning any bulk
#      mutation naming it into a whole-table operation. Sizing the probe on the
#      wrong field is itself destructive, so phase A checks the selector
#      read-only and the run refuses to continue if it matches anything.
#   2. A seeded note is looked up after every probe has run. If any probe had
#      widened to the whole table the note would be gone and the run fails,
#      whatever the affectedCount said.
#
# Only entities whose filter rejects unknown fields are probed. sms, contacts,
# calls and media build their clauses on the Kotlin ContentWhere path, where an
# unrecognised field still collapses to `1=1` — a probe there would become a
# real delete the moment those permissions are granted.
#
# One login per phase, not per case: the device allows 5 logins a minute and a
# per-case loop burns them all on 90-second timeouts. Operations inside a single
# document run in order, so a phase can carry its whole batch.
set -uo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/test-lib.sh" "$@"

# No default port: the caller must name the build under test explicitly.
BASE_URL="${1:?usage: %s <base-url> [client-id]  (port is the one the build under test bound)}"
CLIENT_ID="${2:-plain-mutation-test}"
CLIENT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/gql-client.mjs"
HOST="$(printf '%s' "$BASE_URL" | sed -e 's#^[a-z]*://##' -e 's#:.*##')"
PORT="$(printf '%s' "$BASE_URL" | sed -n 's#.*:\([0-9][0-9]*\)\(/\{0,1\}\).*#\1#p')"

# These probes seed a note and delete things. Running them against a build this
# checkout did not produce would report on the wrong app entirely, so the
# device has to be proven first — see verify_app_under_test.
if [ -z "${PLAIN_SERIAL:-}" ]; then
  echo "!! PLAIN_SERIAL is not set — refusing to mutate an unidentified device" >&2
  exit 1
fi
if ! verify_app_under_test "$PLAIN_SERIAL" "$PORT"; then
  echo "Passed: 0" >&2
  echo "Failed: 1" >&2
  echo "  FAIL [app_under_test]: the device is not running the build this checkout produced" >&2
  exit 1
fi

# `ids:` is supported on notes, feed entries and clipboard. The value cannot
# exist, so every bulk mutation below resolves to zero rows.
PROBE='ids:__plain_probe_no_match__'
# Unique per run so the seed can be cleaned up by title, which keeps the cleanup
# inside the same batch as the probes instead of needing the id first.
SEED="plain-mutation-probe-$$-$(date +%s)"

PASS=0
FAIL=0
ERRORS=""
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if ! command -v node >/dev/null 2>&1; then
  echo "node is required (scripts/gql-client.mjs)" >&2
  exit 1
fi

pass() { PASS=$((PASS + 1)); echo "  PASS [$1] — $2"; }
fail() { FAIL=$((FAIL + 1)); ERRORS="${ERRORS}\n  FAIL [$1]: $2"; echo "  FAIL [$1] — $2"; }

# send <file-with-one-case> <expected-lines> — one login, one request. Case name
# and document come from the two lines of the file.
#
# A phase that comes back short has told us nothing about the data, only about
# the transport. It is retried once and then reported as a failure that quotes
# what the client said, because a silent empty payload reads exactly like "the
# row was deleted" and would turn a flaky login into a fake regression.
send() {
  local q="$1" want="$2" got i
  for i in 1 2; do
    : >"$TMP/err"
    node "$CLIENT" --host "$HOST" --port "$PORT" --client-id "$CLIENT_ID" \
      --queries "$q" ${PLAIN_APPROVE_CMD:+--on-pending "$PLAIN_APPROVE_CMD"} \
      >"$TMP/out" 2>"$TMP/err"
    got=$(wc -l <"$TMP/out" | tr -d ' ')
    [ "$got" -ge "$want" ] && return 0
    echo "    phase $3 got $got of $want result lines (attempt $i); client said: $(tail -1 "$TMP/err")" >&2
    [ "$i" = "1" ] && sleep 15
  done
  return 1
}

# field <line> <key> — pull one key out of a GraphQL payload. Not a sed
# alternation over the value's shape: a `[-0-9]*` branch matches the empty
# string and wins before the real value does, so every number came back blank.
field() {
  printf '%s' "$1" \
    | grep -o "\"$2\":[^,}]*" \
    | head -1 \
    | sed "s/^\"$2\"://" \
    | tr -d '"[] '
}

detail_of() { grep "^raw:$1" "$TMP/out" | head -1 | cut -f3-; }

echo "=== GraphQL mutation Test ==="
echo "URL: ${BASE_URL}/graphql"
echo "Selector: ${PROBE}"
echo ""

# --- phase A: baseline, read-only -------------------------------------------
cat >"$TMP/a.tsv" <<EOF
raw:a_total	{ total: noteCount(query: "") }
raw:a_probe	{ probe: noteCount(query: "${PROBE}") }
EOF
send "$TMP/a.tsv" 2 A
BEFORE=$(field "$(detail_of a_total)" total)
PROBE_HITS=$(field "$(detail_of a_probe)" probe)
if [ -z "$BEFORE" ]; then
  echo "  could not read the baseline note count — the login did not complete" >&2
  tail -6 "$TMP/err" >&2
  exit 1
fi
echo "  baseline: ${BEFORE} note(s); selector matches ${PROBE_HITS}"

# The selector must match nothing. If it matched rows, every probe below would
# be a real mutation and this run has to stop before sending one.
if [ "$PROBE_HITS" != "0" ]; then
  echo "  REFUSING TO RUN: selector ${PROBE} matches ${PROBE_HITS} row(s)," >&2
  echo "  so the probes are not the no-op they are assumed to be." >&2
  exit 1
fi
pass selector "${PROBE} matches 0 rows"
sleep 6

# --- phase B: seed, then every probe, in one ordered document ----------------
cat >"$TMP/b.tsv" <<EOF
raw:b_seed	mutation { seed: createNote(input: { title: "${SEED}", content: "disposable ${SEED}" }) { id } }
raw:b_trashNotes	mutation { trashNotes(query: "${PROBE}") { affectedCount } }
raw:b_restoreNotes	mutation { restoreNotes(query: "${PROBE}") { affectedCount } }
raw:b_deleteNotes	mutation { deleteNotes(query: "${PROBE}") { affectedCount } }
raw:b_deleteFeedEntries	mutation { deleteFeedEntries(query: "${PROBE}") { affectedCount } }
raw:b_markFeedEntriesRead	mutation { markFeedEntriesRead(query: "${PROBE}", read: true) { affectedCount } }
raw:b_deleteClipboardItems	mutation { deleteClipboardItems(query: "${PROBE}") { affectedCount } }
raw:b_saveFeedEntriesToNotes	mutation { saveFeedEntriesToNotes(query: "${PROBE}") }
raw:b_unknownField	mutation { trashNotes(query: "id:__probe__") { affectedCount } }
raw:b_updateNote	mutation { updateNote(id: "__probe__", input: { title: "x", content: "y" }) { id } }
raw:b_updateTag	mutation { updateTag(id: "__probe__", name: "x") { id } }
EOF
send "$TMP/b.tsv" 11 B
SEED_ID=$(field "$(detail_of b_seed)" id)
if [ -z "$SEED_ID" ]; then
  fail seed "createNote returned no id"
  tail -6 "$TMP/err" >&2
  echo ""; echo "--- Summary ---"; echo "Passed: $PASS"; echo "Failed: $((FAIL + 1))"
  exit 1
fi
pass seed "created note ${SEED_ID}"
sleep 6

# Bulk probes. Each expects affectedCount 0, or a refusal from a permission gate
# (which fires before anything is touched).
check_action() { # check_action <case> <label>
  local d
  d="$(detail_of "$1")"
  if printf '%s' "$d" | grep -qE 'no_permission|_disabled'; then
    pass "$2" "refused by the permission gate — nothing touched"
  elif printf '%s' "$d" | grep -q 'affectedCount'; then
    local c; c=$(field "$d" affectedCount)
    if [ "$c" = "0" ]; then pass "$2" "affectedCount 0"
    else fail "$2" "affectedCount $c — the selector was supposed to match nothing"; fi
  else
    fail "$2" "$d"
  fi
}
check_action b_trashNotes           trashNotes
check_action b_restoreNotes         restoreNotes
check_action b_deleteNotes          deleteNotes
check_action b_deleteFeedEntries    deleteFeedEntries
check_action b_markFeedEntriesRead  markFeedEntriesRead
check_action b_deleteClipboardItems deleteClipboardItems

d="$(detail_of b_saveFeedEntriesToNotes)"
if printf '%s' "$d" | grep -q '"saveFeedEntriesToNotes":\[\]'; then
  pass saveFeedEntriesToNotes "returned []"
else
  fail saveFeedEntriesToNotes "$d"
fi

# The regression that motivated this file: an unknown field is refused by name
# instead of silently widening.
d="$(detail_of b_unknownField)"
if printf '%s' "$d" | grep -q 'unsupported note filter: id'; then
  pass rejects_unknown_field 'trashNotes(query: "id:...") refused by name'
else
  fail rejects_unknown_field "$d"
fi

# Updates addressed at a row that cannot exist must refuse, not create one.
for pair in "b_updateNote:updateNote" "b_updateTag:updateTag"; do
  d="$(detail_of "${pair%%:*}")"
  if printf '%s' "$d" | grep -qiE 'not found|no_permission'; then
    pass "${pair##*:}" "refused: the row does not exist"
  else
    fail "${pair##*:}" "$d"
  fi
done
sleep 6

# --- phase C: the invariant -------------------------------------------------
# If any probe above had widened to the whole table, the seeded note is gone.
cat >"$TMP/c.tsv" <<EOF
raw:c_note	{ note(id: "${SEED_ID}") { id } }
raw:c_total	{ total: noteCount(query: "") }
EOF
if ! send "$TMP/c.tsv" 2 C; then
  fail seeded_note_survived "the invariant check could not be sent or answered — see the phase error above"
  fail table_unchanged "the invariant check could not be sent or answered"
  echo ""; echo "--- Summary ---"; echo "Passed: $PASS"; echo "Failed: $FAIL"
  printf "$ERRORS\n"; echo ""
  echo "Some tests failed. Check the output above."
  exit 1
fi
d="$(detail_of c_note)"
# Three outcomes, and conflating them is how a failed request turns into a fake
# "the note was deleted": the id is there, an explicit `null` is there, and
# anything else — no line, an error payload — means the check never ran. Only
# the explicit null is a real regression; the rest must say what it saw.
if printf '%s' "$d" | grep -q "\"$SEED_ID\""; then
  pass seeded_note_survived "note ${SEED_ID} is still there after every probe"
elif printf '%s' "$d" | grep -q '"note":null'; then
  fail seeded_note_survived "note ${SEED_ID} came back null — a probe reached the whole table"
else
  fail seeded_note_survived "the check did not return a usable answer: '${d:-<no line at all>}'"
fi
MID=$(field "$(detail_of c_total)" total)
if [ -z "$MID" ]; then
  fail table_unchanged "could not read the count: '$(detail_of c_total)'"
elif [ "$MID" = "$((BEFORE + 1))" ]; then
  pass table_unchanged "note count is ${MID} — the seed and nothing else"
else
  fail table_unchanged "note count is ${MID}, expected $((BEFORE + 1))"
fi
sleep 6

# --- phase D: clean up the seed, addressed by its unique marker --------------
# `text:` matches the note body, not its title, so SEED goes in the content too.
# Addressing it by marker keeps cleanup in its own batch instead of needing the
# id to be known before the document is built.
cat >"$TMP/d.tsv" <<EOF
raw:d_trash	mutation { trashNotes(query: "text:${SEED}") { affectedCount } }
raw:d_delete	mutation { deleteNotes(query: "text:${SEED}") { affectedCount } }
EOF
send "$TMP/d.tsv" 2 D
T=$(field "$(detail_of d_trash)" affectedCount)
R=$(field "$(detail_of d_delete)" affectedCount)
if [ "$T" = "1" ] && [ "$R" = "1" ]; then
  pass cleaned_up "seed trashed and deleted (1 and 1)"
else
  fail cleaned_up "cleanup hit ${T} and ${R}, expected 1 and 1"
fi

echo ""
echo "--- Summary ---"
echo "Passed: $PASS"
echo "Failed: $FAIL"
if [ -n "$ERRORS" ]; then printf "$ERRORS\n"; fi
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "All GraphQL mutation tests passed!"
  exit 0
fi
echo "Some tests failed. Check the output above."
exit 1
