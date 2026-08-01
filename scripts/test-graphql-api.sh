#!/bin/bash
# GraphQL API Test Script for iOS App
# Usage: ./scripts/test-graphql-api.sh [URL] [TOKEN] [CLIENT_ID]
# Default: http://127.0.0.1:8080 with token from web login

BASE_URL="${1:-http://127.0.0.1:8080}"
TOKEN="${2:-f8g+0ySbsQPvLkrnuF6H3zarotMgRvvi0tBuWSDfzrw=}"
CLIENT_ID="${3:-aUznhGpBKsqr1pkypob5S2}"

PASS=0
FAIL=0
ERRORS=""

gql() {
    local query_name="$1"
    local query="$2"
    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/graphql" \
        -H "c-id: ${CLIENT_ID}" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H "Content-Type: application/json" \
        --data "{\"query\":\"${query}\"}" 2>&1)

    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | head -n -1)

    if [ "$http_code" = "200" ]; then
        if echo "$body" | grep -q '"errors"'; then
            FAIL=$((FAIL + 1))
            ERRORS="${ERRORS}\n  FAIL [$query_name]: GraphQL errors in response: $(echo "$body" | head -c 200)"
            echo "  FAIL [$query_name] — GraphQL errors"
        else
            PASS=$((PASS + 1))
            echo "  PASS [$query_name] — 200 OK"
        fi
    elif [ "$http_code" = "500" ]; then
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$query_name]: 500 Internal Server Error"
        echo "  FAIL [$query_name] — 500 Internal Server Error"
    else
        FAIL=$((FAIL + 1))
        ERRORS="${ERRORS}\n  FAIL [$query_name]: HTTP $http_code"
        echo "  FAIL [$query_name] — HTTP $http_code"
    fi
}

echo "=== GraphQL API Test ==="
echo "URL: ${BASE_URL}/graphql"
echo "Client ID: ${CLIENT_ID}"
echo ""

echo "--- Queries ---"

gql "app" "{ app { appVersion deviceName battery clientId urlToken httpPort httpsPort } }"
gql "app_permissions" "{ app { permissions } }"
gql "peers" "{ peers { id name ip status online port deviceType } }"
gql "chatChannels" "{ chatChannels { id type name createdAt updatedAt } }"
gql "feeds" "{ feeds { id title url } }"
gql "feedEntries" "{ feedEntries(feedId: \"\") { id title } }"
gql "notes" "{ notes { id title content } }"
gql "images" "{ images(limit: 1) { id name size width height } }"
gql "videos" "{ videos(limit: 1) { id name size duration width height } }"
gql "audios" "{ audios(limit: 1) { id name size duration } }"
gql "contacts" "{ contacts(limit: 1) { id name phoneNumbers { number } } }"
gql "calls" "{ calls(limit: 1) { id number name } }"
gql "files" "{ files(path: \"\") { id name size } }"
gql "bookmarks" "{ bookmarks { id title url } }"
gql "bookmarkGroups" "{ bookmarkGroups { id name } }"
gql "tags" "{ tags { id name } }"
gql "packages" "{ packages { id name versionName } }"
gql "dbTables" "{ dbTables { name count } }"
gql "notifications" "{ notifications(limit: 1) { id title text } }"
gql "battery" "{ battery { level charging } }"
gql "rules" "{ rules { id } }"
gql "pomodoroToday" "{ pomodoroToday { date completedCycles } }"
gql "pomodoroRuntimeInfo" "{ pomodoroRuntimeInfo { running } }"
gql "pomodoroSettings" "{ pomodoroSettings { workDuration } }"
gql "imageEditorProjects" "{ imageEditorProjects { id name } }"
gql "dataStore" "{ dataStore(key: \"\") }"
gql "appLogs" "{ appLogs(limit: 1) { id level tag message } }"
gql "ssdpDevices" "{ ssdpDevices { id name } }"
gql "castDevices" "{ castDevices { id name } }"
gql "storageMounts" "{ storageMounts { path label } }"

echo ""
echo "--- Summary ---"
echo "Passed: $PASS"
echo "Failed: $FAIL"
if [ -n "$ERRORS" ]; then
    echo -e "\nFailures:$ERRORS"
fi
echo ""
if [ "$FAIL" -eq 0 ]; then
    echo "All GraphQL API tests passed!"
    exit 0
else
    echo "Some tests failed. Check the output above."
    exit 1
fi
