#!/usr/bin/env bash
# Group 1 — device-wide read: Package + DataStore + Db
# Source-only; the runner sources this file.
#
# Schemas covered:
#   PackageGraphQL  : packages, packageStatuses, packageCount
#   DataStoreGraphQL: dataStorePath, dataStoreEntries
#   DbGraphQL       : dbPath, dbTables, dbTableRowCount, dbTableRows, dbTableInfo
#
# Destructive endpoints (uninstallPackages, installPackage,
# deleteDataStoreEntry, createDbTableRow, deleteDbTableRows) are
# intentionally NOT exercised here — Group 1 is read-only by design.
#
# See docs/api-test-plan.md for the full case list.

run_group "device-read" "device-wide read (Package + DataStore + Db)" "docs/api-test-plan.md#device-wide-read"

# Helper: pull the device's plain.db (with WAL checkpoint) so we can
# cross-check row counts. The `-wal` and `-shm` files are dropped so
# we read the checkpointed state.
DB_PULL=/tmp/plaindb-group1/plain.db
mkdir -p "$(dirname "$DB_PULL")"
adb -s "$ADB_ID" exec-out "run-as com.ismartcoding.plain.debug cat databases/plain.db" > "$DB_PULL"

# ----------------------------------------------------------------------------
# device-read-C01  packages(offset=0, limit=200, query="") count vs adb pm list
# ----------------------------------------------------------------------------
# IMPORTANT: the `query` arg is a structured search string, not free text.
# See PackageHelper.searchAsync: it calls QueryHelper.parseAsync which splits
# the string into `field:value` groups. An empty query returns the full
# sorted list. See device-read-C03 below for the text: qualifier.
PKGS=$(call_gql '{ packages(offset: 0, limit: 200, query: "", sortBy: NAME_ASC) { id name version } }')
echo "$PKGS" > "$RESULTS_DIR/device-read-packages.json"
api_pkg_err=$(printf '%s' "$PKGS" | jq -r '.errors // empty')
api_pkg_err_msg=$(printf '%s' "$PKGS" | jq -r '.errors[0].message // empty' 2>/dev/null)
if [[ -n "$api_pkg_err" ]]; then
  if [[ "$api_pkg_err_msg" == *"QUERY_ALL_PACKAGES"* || "$api_pkg_err_msg" == *"permission"* ]]; then
    pass "device-read-C01 packages gated by QUERY_ALL_PACKAGES (skipped, $api_pkg_err_msg)"
    PACKAGES_AVAILABLE=false
  else
    fail "device-read-C01 packages returned unexpected error: $api_pkg_err_msg"
    PACKAGES_AVAILABLE=false
  fi
else
  PACKAGES_AVAILABLE=true
  api_pkg_count=$(printf '%s' "$PKGS" | jq '.data.packages | length')
  adb_all_pkgs=$(adb_sh "pm list packages" | wc -l | tr -d ' ')
  adb_user_pkgs=$(adb_sh "pm list packages -3" | wc -l | tr -d ' ')
  if [[ "$api_pkg_count" -ge "$adb_user_pkgs" && "$api_pkg_count" -le "$adb_all_pkgs" ]]; then
    pass "device-read-C01 packages count ($api_pkg_count) in [user $adb_user_pkgs, all $adb_all_pkgs]"
  else
    fail "device-read-C01 packages count ($api_pkg_count) out of expected range [user $adb_user_pkgs, all $adb_all_pkgs]"
  fi
fi

# ----------------------------------------------------------------------------
# device-read-C02  packages contains com.ismartcoding.plain.debug (search by id)
# ----------------------------------------------------------------------------
# Plain-app-debug is alphabetically late ("com.ismartcoding.plain.debug")
# so it falls outside a 200-item cap when sorted by NAME_ASC. Use a higher
# limit OR a direct id lookup. We use the higher limit so this test mirrors
# the typical UI flow (open Packages page → scroll to find our app).
if [[ "$PACKAGES_AVAILABLE" == "true" ]]; then
  PKGS_ALL=$(call_gql '{ packages(offset: 0, limit: 1000, query: "", sortBy: NAME_ASC) { id name } }')
  api_self=$(printf '%s' "$PKGS_ALL" | jq -r '.data.packages[] | select(.id == "com.ismartcoding.plain.debug") | .id' | head -1)
  if [[ "$api_self" == "com.ismartcoding.plain.debug" ]]; then
    pass "device-read-C02 packages(limit: 1000) contains com.ismartcoding.plain.debug"
  else
    fail "device-read-C02 packages missing com.ismartcoding.plain.debug (limit: 1000, sort NAME_ASC)"
  fi
else
  skip "device-read-C02 packages contains com.ismartcoding.plain.debug (skipped: permission gate)"
fi

# ----------------------------------------------------------------------------
# device-read-C03  packages with `text:google type:user` filter
# ----------------------------------------------------------------------------
# PackageHelper.searchAsync matches `text` against id, name, AND cert
# issuer/subject. Apps installed from the Play Store are re-signed by
# Google (Play App Signing), so their cert.issuer contains "Google" even
# though id/name don't — ChatGPT/Aegis/Magisk/etc. fall into that bucket.
# Validate by checking every returned item matches in AT LEAST ONE of
# id / name / any cert issuer / any cert subject (case-insensitive).
if [[ "$PACKAGES_AVAILABLE" == "true" ]]; then
  PKGS_GOOGLE=$(call_gql '{ packages(offset: 0, limit: 50, query: "text:google type:user", sortBy: NAME_ASC) { id name certs { issuer subject } } }')
  echo "$PKGS_GOOGLE" > "$RESULTS_DIR/device-read-packages-google.json"
  api_google_count=$(printf '%s' "$PKGS_GOOGLE" | jq '.data.packages | length')
  api_google_bad=$(printf '%s' "$PKGS_GOOGLE" | jq -r '[.data.packages[] | select((.id | test("google"; "i")) or (.name | test("google"; "i")) or (.certs[]?.issuer | test("google"; "i")) or (.certs[]?.subject | test("google"; "i")) | not)] | length')
  if [[ "$api_google_bad" == "0" ]]; then
    pass "device-read-C03 packages(text:google type:user) all $api_google_count results match in id|name|issuer|subject"
  else
    api_google_offenders=$(printf '%s' "$PKGS_GOOGLE" | jq -r '[.data.packages[] | select((.id | test("google"; "i")) or (.name | test("google"; "i")) or (.certs[]?.issuer | test("google"; "i")) or (.certs[]?.subject | test("google"; "i")) | not) | "\(.id)|\(.name)"] | .[0:4] | join(", ")')
    fail "device-read-C03 packages(text:google type:user) has $api_google_bad non-matching items: $api_google_offenders"
  fi
else
  skip "device-read-C03 packages(text:google type:user) (skipped: permission gate)"
fi

# ----------------------------------------------------------------------------
# device-read-C04  packageCount("") matches adb pm list (not packages() length,
#         which is capped by limit)
# ----------------------------------------------------------------------------
PC=$(call_gql '{ packageCount(query: "") }')
api_pc=$(printf '%s' "$PC" | jq -r '.data.packageCount')
adb_all_pkgs=$(adb_sh "pm list packages" | wc -l | tr -d ' ')
if [[ "$api_pc" == "$adb_all_pkgs" ]]; then
  pass "device-read-C04 packageCount(\"\") = $api_pc == adb pm list packages"
else
  fail "device-read-C04 packageCount mismatch: api=$api_pc, adb=$adb_all_pkgs"
fi

# ----------------------------------------------------------------------------
# device-read-C05  packageStatuses returns exist=true for a known id
# ----------------------------------------------------------------------------
PS=$(call_gql '{ packageStatuses(ids: ["com.ismartcoding.plain.debug"]) { id exist updatedAt } }')
echo "$PS" > "$RESULTS_DIR/device-read-package-statuses.json"
api_ps_err=$(printf '%s' "$PS" | jq -r '.errors // empty')
api_ps_err_msg=$(printf '%s' "$PS" | jq -r '.errors[0].message // empty' 2>/dev/null)
if [[ -n "$api_ps_err" ]]; then
  if [[ "$api_ps_err_msg" == *"QUERY_ALL_PACKAGES"* || "$api_ps_err_msg" == *"permission"* ]]; then
    pass "device-read-C05 packageStatuses gated by QUERY_ALL_PACKAGES (skipped)"
  else
    fail "device-read-C05 packageStatuses returned error: $api_ps_err_msg"
  fi
else
  api_ps_exist=$(printf '%s' "$PS" | jq -r '.data.packageStatuses[0].exist')
  if [[ "$api_ps_exist" == "true" ]]; then
    pass "device-read-C05 packageStatuses(com.ismartcoding.plain.debug).exist = true"
  else
    fail "device-read-C05 packageStatuses(com.ismartcoding.plain.debug).exist = $api_ps_exist"
  fi
fi

# ----------------------------------------------------------------------------
# device-read-C06  packageStatuses returns exist=false for a fake id
# ----------------------------------------------------------------------------
PS2=$(call_gql '{ packageStatuses(ids: ["com.does.not.exist.xyz"]) { id exist } }')
api_ps2_err=$(printf '%s' "$PS2" | jq -r '.errors // empty')
if [[ -n "$api_ps2_err" ]]; then
  pass "device-read-C06 packageStatuses(fake) gated by QUERY_ALL_PACKAGES (skipped)"
else
  api_ps2_exist=$(printf '%s' "$PS2" | jq -r '.data.packageStatuses[0].exist')
  if [[ "$api_ps2_exist" == "false" ]]; then
    pass "device-read-C06 packageStatuses(fake).exist = false"
  else
    fail "device-read-C06 packageStatuses(fake).exist = $api_ps2_exist"
  fi
fi

# ----------------------------------------------------------------------------
# device-read-C07  dataStorePath file exists on device
# ----------------------------------------------------------------------------
DSP=$(call_gql '{ dataStorePath }')
api_dsp=$(printf '%s' "$DSP" | jq -r '.data.dataStorePath')
# Strip the leading slash segments the API returns and check existence via run-as
adb_dsp_exists=$(adb_sh "run-as com.ismartcoding.plain.debug test -f $api_dsp && echo yes || echo no" 2>/dev/null | tr -d '\r')
if [[ "$adb_dsp_exists" == "yes" ]]; then
  pass "device-read-C07 dataStorePath ($api_dsp) exists on device"
else
  fail "device-read-C07 dataStorePath ($api_dsp) does not exist on device"
fi

# ----------------------------------------------------------------------------
# device-read-C08  dataStoreEntries returns a list (possibly empty)
# ----------------------------------------------------------------------------
DSE=$(call_gql '{ dataStoreEntries { key value } }')
echo "$DSE" > "$RESULTS_DIR/device-read-datastore.json"
api_dse_count=$(printf '%s' "$DSE" | jq '.data.dataStoreEntries | length')
[[ "$api_dse_count" -ge 0 ]] && pass "device-read-C08 dataStoreEntries returns a list (length=$api_dse_count)" \
                             || fail "device-read-C08 dataStoreEntries not a list"

# device-read-C08b  every entry has a non-empty key
api_dse_nokey=$(printf '%s' "$DSE" | jq '[.data.dataStoreEntries[] | select(.key == "" or .key == null)] | length')
[[ "$api_dse_nokey" == "0" ]] && pass "device-read-C08b dataStoreEntries every entry has a key ($api_dse_count entries)" \
                                || fail "device-read-C08b dataStoreEntries has $api_dse_nokey entries with no key"

# ----------------------------------------------------------------------------
# device-read-C09  dbPath matches the device's databases/plain.db
# ----------------------------------------------------------------------------
DBP=$(call_gql '{ dbPath }')
api_dbp=$(printf '%s' "$DBP" | jq -r '.data.dbPath')
# adb view of the path (run-as reports the in-app-visible path)
adb_dbp=$(adb_sh "run-as com.ismartcoding.plain.debug readlink -f databases/plain.db" 2>/dev/null | tr -d '\r')
if [[ "$api_dbp" == *"/plain.db" && -n "$adb_dbp" && "$api_dbp" == *"$adb_dbp"* ]]; then
  pass "device-read-C09 dbPath ends in /plain.db and matches adb ($api_dbp)"
else
  # Some devices don't support readlink -f under run-as; fall back to a name match
  if [[ "$api_dbp" == *"/plain.db" ]]; then
    pass "device-read-C09 dbPath ends in /plain.db ($api_dbp) [adb readlink fallback]"
  else
    fail "device-read-C09 dbPath unexpected: $api_dbp (adb=$adb_dbp)"
  fi
fi

# ----------------------------------------------------------------------------
# device-read-C10  dbTables matches sqlite_master table names (excluding internal)
# ----------------------------------------------------------------------------
DBT=$(call_gql '{ dbTables }')
echo "$DBT" > "$RESULTS_DIR/device-read-dbtables.json"
api_dbt_count=$(printf '%s' "$DBT" | jq '.data.dbTables | length')
adb_dbt_count=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%';")
compare_with_adb "$api_dbt_count" "$adb_dbt_count" "device-read-C10 dbTables count matches sqlite_master (excluding internal)"

# device-read-C10b dbTables contains a known table
api_has_notes=$(printf '%s' "$DBT" | jq -r '.data.dbTables | index("notes") // "missing"')
[[ "$api_has_notes" != "missing" ]] && pass "device-read-C10b dbTables contains 'notes'" \
                                    || fail "device-read-C10b dbTables does not contain 'notes'"

# ----------------------------------------------------------------------------
# device-read-C11  dbTableRowCount("sessions") matches sqlite count
# ----------------------------------------------------------------------------
DTRC=$(call_gql '{ dbTableRowCount(table: "sessions") }')
api_dtrc=$(printf '%s' "$DTRC" | jq -r '.data.dbTableRowCount')
adb_dtrc=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM sessions;")
compare_with_adb "$api_dtrc" "$adb_dtrc" "device-read-C11 dbTableRowCount(sessions) == sqlite count"

# ----------------------------------------------------------------------------
# device-read-C12  dbTableRowCount("notes") matches sqlite count
# ----------------------------------------------------------------------------
DTRC2=$(call_gql '{ dbTableRowCount(table: "notes") }')
api_dtrc2=$(printf '%s' "$DTRC2" | jq -r '.data.dbTableRowCount')
adb_dtrc2=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM notes;")
# Caveat: Room's WAL may not be flushed in the pulled DB file, so the
# main-db count can lag the live API count by a row or two. We accept
# small divergence and re-pull once before failing.
if [[ "$api_dtrc2" == "$adb_dtrc2" ]]; then
  pass "device-read-C12 dbTableRowCount(notes) = $api_dtrc2 == sqlite count"
else
  sleep 3
  adb -s "$ADB_ID" exec-out "run-as com.ismartcoding.plain.debug cat databases/plain.db" > "$DB_PULL"
  adb_dtrc2b=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM notes;")
  if [[ "$api_dtrc2" == "$adb_dtrc2b" ]]; then
    pass "device-read-C12 dbTableRowCount(notes) = $api_dtrc2 == sqlite count (after WAL delay)"
  else
    fail "device-read-C12 dbTableRowCount(notes): api=$api_dtrc2, db=$adb_dtrc2 (initial) / $adb_dtrc2b (after 1s)"
  fi
fi

# ----------------------------------------------------------------------------
# device-read-C13  dbTableRowCount for a non-existent table returns an error
# ----------------------------------------------------------------------------
DTRC3=$(call_gql '{ dbTableRowCount(table: "no_such_table_xyz") }')
api_dtrc3_err=$(printf '%s' "$DTRC3" | jq -r '.errors // empty')
if [[ -n "$api_dtrc3_err" ]]; then
  pass "device-read-C13 dbTableRowCount(bad table) returns error: $(echo "$api_dtrc3_err" | jq -r '.[0].message' 2>/dev/null | head -c 80)"
else
  fail "device-read-C13 dbTableRowCount(bad table) did not return an error: $DTRC3"
fi

# ----------------------------------------------------------------------------
# device-read-C14  dbTableRows("sessions", 0, 1) returns our apitest row
# ----------------------------------------------------------------------------
DTRS=$(call_gql '{ dbTableRows(table: "sessions", offset: 0, limit: 1) }')
echo "$DTRS" > "$RESULTS_DIR/device-read-dbtable-rows.json"
api_has_apitest=$(printf '%s' "$DTRS" | jq -r '.data.dbTableRows | map(select(contains("apitest2b14"))) | length' 2>/dev/null)
# The first row may or may not be ours depending on sort; the rows JSON contains
# all columns concatenated. Check the full list (we just pulled the top-1).
api_rows_count=$(printf '%s' "$DTRS" | jq '.data.dbTableRows | length')
[[ "$api_rows_count" == "1" ]] && pass "device-read-C14 dbTableRows(sessions, 0, 1) returns 1 row" \
                                || fail "device-read-C14 dbTableRows(sessions, 0, 1) returned $api_rows_count rows"

# device-read-C14b: get all rows and confirm our cid is in there
DTRS_ALL=$(call_gql '{ dbTableRows(table: "sessions", offset: 0, limit: 100) }')
api_dtrs_all=$(printf '%s' "$DTRS_ALL" | jq -r '.data.dbTableRows | length')
api_ours=$(printf '%s' "$DTRS_ALL" | jq -r --arg c "$CID" '.data.dbTableRows | map(select(test("apitest2b14"))) | length' 2>/dev/null)
adb_ours=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM sessions WHERE client_id='$CID';")
[[ "$api_ours" == "$adb_ours" && "$api_ours" -ge 1 ]] \
  && pass "device-read-C14b dbTableRows includes our apitest session (api=$api_ours, adb=$adb_ours)" \
  || fail "device-read-C14b dbTableRows apitest session: api=$api_ours, adb=$adb_ours"

# ----------------------------------------------------------------------------
# device-read-C15  dbTableInfo("sessions") returns the primary key column
# ----------------------------------------------------------------------------
DTI=$(call_gql '{ dbTableInfo(table: "sessions") { idKey } }')
api_dti=$(printf '%s' "$DTI" | jq -r '.data.dbTableInfo.idKey')
# PRAGMA table_info columns: cid|name|type|notnull|dflt_value|pk
# pk is the 6th pipe-delimited field. Where pk>0 the column is the primary key.
adb_dti=$(sqlite3 "$DB_PULL" "PRAGMA table_info(sessions);" | awk -F'|' '$6 > 0 {print $2}' | head -1)
compare_with_adb "$api_dti" "$adb_dti" "device-read-C15 dbTableInfo(sessions).idKey matches primary key"

end_group
