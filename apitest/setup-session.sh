#!/usr/bin/env bash
# One-time helper to mint a TYPE_CUSTOM session directly in plain.db.
#
# USE: when the operator does not have an API token from the Web UI and
# wants the apitest harness to authenticate against /graphql.
#
# What it does:
#   1. force-stops the app (releases the WAL writer)
#   2. pulls databases/plain.db to /tmp
#   3. INSERTs a new TYPE_CUSTOM session row
#   4. pushes the modified db back
#   5. restarts the app
#   6. prints the generated cid + token to stdout
#
# The token is a real ChaCha20 key (32 random bytes, base64), same shape
# the Web UI's "API Access" flow creates. Once the app is back up, the
# harness can hit POST /graphql with:
#     c-id: <printed cid>
#     Authorization: Bearer <printed token>
#
# No data is destroyed — we only insert a new row in `sessions`.

set -euo pipefail
ADB_ID="${ADB_ID:-adb-47260DLAQ003RB-7TItk3._adb-tls-connect._tcp}"
PKG="${PKG:-com.ismartcoding.plain.debug}"
LOCAL_DB="/tmp/plaindb-setup/plain.db"
mkdir -p "$(dirname "$LOCAL_DB")"

echo "==> force-stop $PKG (releases SQLite WAL writer)"
adb -s "$ADB_ID" shell "am force-stop $PKG"

echo "==> pull databases/plain.db -> $LOCAL_DB"
adb -s "$ADB_ID" exec-out "run-as $PKG cat databases/plain.db" > "$LOCAL_DB"
[[ -s "$LOCAL_DB" ]] || { echo "pull failed"; exit 1; }

CID="apitest$(openssl rand -hex 2)"
TOKEN=$(head -c 32 /dev/urandom | base64 | tr -d '\n=')
NOW=$(date -u +%Y-%m-%dT%H:%M:%S.000Z)

echo "==> INSERT TYPE_CUSTOM session: client_id=$CID"
sqlite3 "$LOCAL_DB" <<SQL
INSERT INTO sessions
  (client_id, name, type, client_ip, os_name, os_version, browser_name, browser_version, token, last_active_at, created_at, updated_at)
VALUES
  ('$CID', 'apitest', 'custom', '127.0.0.1', 'apitest', '1.0', 'apitest', '1.0', '$TOKEN', '$NOW', '$NOW', '$NOW');
SQL

echo "==> push modified db back"
adb -s "$ADB_ID" push "$LOCAL_DB" "/data/local/tmp/plain.db.new" >/dev/null
adb -s "$ADB_ID" shell "run-as $PKG cp /data/local/tmp/plain.db.new databases/plain.db" 2>&1 | head -3
adb -s "$ADB_ID" shell "run-as $PKG rm -f databases/plain.db-shm databases/plain.db-wal" 2>&1 | head -3
adb -s "$ADB_ID" shell "rm -f /data/local/tmp/plain.db.new"

echo "==> launch app"
adb -s "$ADB_ID" shell "monkey -p $PKG -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1

echo ""
echo "================================================================"
echo "  Session created. Paste these into apitest/config.json:"
echo ""
echo "    \"cid\":   \"$CID\""
echo "    \"token\": \"$TOKEN\""
echo "================================================================"
