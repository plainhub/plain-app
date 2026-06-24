#!/usr/bin/env bash
# Shared helpers for the apitest harness.
# Source this from every group script. Never run it directly.

set -o pipefail

APITEST_ROOT="${APITEST_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
CONFIG_FILE="${APITEST_ROOT}/config.json"
RESULTS_DIR="${APITEST_ROOT}/results"

mkdir -p "$RESULTS_DIR"

# load_config: parses config.json, exports ADB_ID, URL, CID, TOKEN
load_config() {
  if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "config.json missing at $CONFIG_FILE" >&2
    return 1
  fi
  ADB_ID=$(jq -r '.adb_id' "$CONFIG_FILE")
  URL=$(jq -r '.url' "$CONFIG_FILE")
  CID=$(jq -r '.cid' "$CONFIG_FILE")
  TOKEN=$(jq -r '.token' "$CONFIG_FILE")
  if [[ "$CID" == "REPLACE_ME" || "$TOKEN" == "REPLACE_ME" ]]; then
    echo "config.json still has REPLACE_ME placeholders. Fill cid + token first." >&2
    return 1
  fi
  export ADB_ID URL CID TOKEN
}

# call_gql QUERY_STRING
# Prints the raw JSON response from POST /graphql.
call_gql() {
  local query="$1"
  curl -sS -X POST "$URL" \
    -H "c-id: $CID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    --data "{\"query\":$(jq -nc --arg q "$query" '$q')}"
}

# call_gql_to FILE QUERY_STRING
# Same as call_gql but also writes the response to FILE.
call_gql_to() {
  local out="$1"
  local query="$2"
  call_gql "$query" > "$out"
  cat "$out"
}

# adb_sh CMD...
# Shorthand for `adb -s $ADB_ID shell ...`
adb_sh() {
  adb -s "$ADB_ID" shell "$@"
}

# adb_get_property PROP  e.g. ro.build.version.release
adb_get_property() {
  adb_sh "getprop $1" 2>/dev/null | tr -d '\r'
}

# Pass / fail helpers --------------------------------------------------------
PASS_COUNT=0
FAIL_COUNT=0
CURRENT_CASE=""

pass() { echo "  PASS  $1"; PASS_COUNT=$((PASS_COUNT+1)); }
fail() { echo "  FAIL  $1"; FAIL_COUNT=$((FAIL_COUNT+1)); }
skip() { echo "  SKIP  $1"; }

# assert_jq RESPONSE_JSON FILTER EXPECTED CASE_NAME
# FILTER is a jq expression evaluated against the response. The result is
# compared (string equality) against EXPECTED. Used for the "API returned
# what we expected" half of each case.
assert_jq() {
  local response="$1" filter="$2" expected="$3" name="$4"
  local actual
  actual=$(printf '%s' "$response" | jq -r "$filter" 2>/dev/null)
  if [[ "$actual" == "$expected" ]]; then
    pass "$name (jq: $filter = $expected)"
  else
    fail "$name (jq: $filter expected '$expected' got '$actual')"
  fi
}

# assert_jq_truthy RESPONSE_JSON FILTER CASE_NAME
# Same as assert_jq but only checks that the filter is non-null and non-false.
assert_jq_truthy() {
  local response="$1" filter="$2" name="$3"
  local actual
  actual=$(printf '%s' "$response" | jq -r "$filter" 2>/dev/null)
  if [[ -n "$actual" && "$actual" != "null" && "$actual" != "false" ]]; then
    pass "$name (jq: $filter truthy → $actual)"
  else
    fail "$name (jq: $filter expected truthy got '$actual')"
  fi
}

# assert_jq_match RESPONSE_JSON FILTER REGEX CASE_NAME
assert_jq_match() {
  local response="$1" filter="$2" regex="$3" name="$4"
  local actual
  actual=$(printf '%s' "$response" | jq -r "$filter" 2>/dev/null)
  if [[ "$actual" =~ $regex ]]; then
    pass "$name (jq: $filter matches /$regex/ → $actual)"
  else
    fail "$name (jq: $filter expected /$regex/ got '$actual')"
  fi
}

# compare_with_adb API_VALUE ADB_VALUE CASE_NAME
# Pass when the two strings are equal. Used to confirm "API value matches
# what we extract from the device via adb".
compare_with_adb() {
  local api_value="$1" adb_value="$2" name="$3"
  if [[ "$api_value" == "$adb_value" ]]; then
    pass "$name (api == adb → $api_value)"
  else
    fail "$name (api '$api_value' != adb '$adb_value')"
  fi
}

# Header printer for a group
print_group_header() {
  local num="$1" name="$2"
  echo ""
  echo "================================================================"
  echo "  Group $num: $name"
  echo "================================================================"
}

# run_group GROUP_ID GROUP_NAME GROUP_DOC
# Scaffolding: prints header, runs the body, prints footer, returns pass/fail count.
run_group() {
  local num="$1" name="$2" doc="$3"
  print_group_header "$num" "$name"
  echo "  (cases below — see $doc for full description)"
  PASS_COUNT=0
  FAIL_COUNT=0
}

end_group() {
  echo "----------------------------------------------------------------"
  echo "  Result: $PASS_COUNT pass / $FAIL_COUNT fail"
  echo "================================================================"
}
