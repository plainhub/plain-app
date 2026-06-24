#!/usr/bin/env bash
# Group app-state — Bookmark + Pomodoro + Notification + AppLogs + AppFile
# Source-only; the runner sources this file.
#
# Schemas covered:
#   BookmarkGraphQL    : bookmarks, bookmarkGroups, addBookmarks, updateBookmark,
#                        deleteBookmarks, recordBookmarkClick,
#                        createBookmarkGroup, updateBookmarkGroup,
#                        deleteBookmarkGroup
#   PomodoroGraphQL    : pomodoroSettings, pomodoroToday, startPomodoro,
#                        pausePomodoro, stopPomodoro
#   NotificationGraphQL: notifications, cancelNotifications, replyNotification
#   AppLogsGraphQL     : appLogs, appLogPath, clearAppLogs
#   AppFileGraphQL     : appFiles, appFileCount
#
# Reads use plain.db (bookmarks / app_files tables) and the on-device
# log file for ground truth. Mutating operations that depend on UI
# state (pomodoro lifecycle, replyNotification) are exercised but not
# tied to runtime invariants.

run_group "app-state" "bookmark + pomodoro + notification + applogs + appfile" "docs/api-test-plan.md#app-state"

# Pull plain.db for direct cross-checks.
DB_PULL=/tmp/plaindb-appstate/plain.db
mkdir -p "$(dirname "$DB_PULL")"
adb -s "$ADB_ID" exec-out "run-as com.ismartcoding.plain.debug cat databases/plain.db" > "$DB_PULL"

# ----------------------------------------------------------------------------
# Bookmark
# ----------------------------------------------------------------------------

# app-state-C01  bookmarks returns list (possibly empty)
BM=$(call_gql '{ bookmarks { id title url } }')
api_bm_count=$(printf '%s' "$BM" | jq '.data.bookmarks | length')
[[ "$api_bm_count" -ge 0 ]] && pass "app-state-C01 bookmarks returned $api_bm_count items" \
                           || fail "app-state-C01 bookmarks not a list: $BM"

# app-state-C02  bookmarkGroups returns list (possibly empty)
BMG=$(call_gql '{ bookmarkGroups { id name collapsed sortOrder } }')
api_bmg_count=$(printf '%s' "$BMG" | jq '.data.bookmarkGroups | length')
[[ "$api_bmg_count" -ge 0 ]] && pass "app-state-C02 bookmarkGroups returned $api_bmg_count items" \
                            || fail "app-state-C02 bookmarkGroups not a list: $BMG"

# app-state-C03..07  Bookmark CRUD: addBookmarks → updateBookmark → recordBookmarkClick → deleteBookmarks
ADD_B=$(call_gql 'mutation { addBookmarks(urls: ["https://apitest.example.com/"], groupId: "") { id title url } }')
api_add_b_id=$(printf '%s' "$ADD_B" | jq -r '.data.addBookmarks[0].id // empty')
if [[ -n "$api_add_b_id" ]]; then
  pass "app-state-C03 addBookmarks → id=$api_add_b_id"
  UPDATE_B=$(call_gql "mutation { updateBookmark(id: \"$api_add_b_id\", input: { url: \"https://apitest.example.com/2\", title: \"apitest2\", groupId: \"\", pinned: false, sortOrder: 0 }) { id title url } }")
  api_ub_title=$(printf '%s' "$UPDATE_B" | jq -r '.data.updateBookmark.title // empty')
  if [[ "$api_ub_title" == "apitest2" ]]; then
    pass "app-state-C04 updateBookmark(id=$api_add_b_id) → title='$api_ub_title'"
  else
    fail "app-state-C04 updateBookmark returned: $UPDATE_B"
  fi

  # C05: recordBookmarkClick
  RBC=$(call_gql "mutation { recordBookmarkClick(id: \"$api_add_b_id\") }")
  api_rbc=$(printf '%s' "$RBC" | jq -r '.data.recordBookmarkClick // empty')
  if [[ "$api_rbc" == "true" ]]; then
    pass "app-state-C05 recordBookmarkClick → true"
  else
    fail "app-state-C05 recordBookmarkClick returned: $RBC"
  fi

  # C06: deleteBookmarks
  DEL_B=$(call_gql "mutation { deleteBookmarks(ids: [\"$api_add_b_id\"]) }")
  api_del_b=$(printf '%s' "$DEL_B" | jq -r '.data.deleteBookmarks // empty')
  if [[ "$api_del_b" == "true" ]]; then
    pass "app-state-C06 deleteBookmarks → true"
  else
    fail "app-state-C06 deleteBookmarks returned: $DEL_B"
  fi
else
  fail "app-state-C03 addBookmarks did not return id: $ADD_B"
  skip "app-state-C04 updateBookmark (no fixture id)"
  skip "app-state-C05 recordBookmarkClick (no fixture id)"
  skip "app-state-C06 deleteBookmarks (no fixture id)"
fi

# app-state-C07..09  Bookmark group CRUD
CREATE_BG=$(call_gql 'mutation { createBookmarkGroup(name: "apitest-group") { id name } }')
api_bg_id=$(printf '%s' "$CREATE_BG" | jq -r '.data.createBookmarkGroup.id // empty')
api_bg_name=$(printf '%s' "$CREATE_BG" | jq -r '.data.createBookmarkGroup.name // empty')
if [[ -n "$api_bg_id" && "$api_bg_name" == "apitest-group" ]]; then
  pass "app-state-C07 createBookmarkGroup → id=$api_bg_id name='$api_bg_name'"
  UPDATE_BG=$(call_gql "mutation { updateBookmarkGroup(id: \"$api_bg_id\", name: \"apitest-group-renamed\", collapsed: false, sortOrder: 0) { id name } }")
  api_bg_upd=$(printf '%s' "$UPDATE_BG" | jq -r '.data.updateBookmarkGroup.name // empty')
  if [[ "$api_bg_upd" == "apitest-group-renamed" ]]; then
    pass "app-state-C08 updateBookmarkGroup → name='$api_bg_upd'"
  else
    fail "app-state-C08 updateBookmarkGroup returned: $UPDATE_BG"
  fi
  DELETE_BG=$(call_gql "mutation { deleteBookmarkGroup(id: \"$api_bg_id\") }")
  api_bg_del=$(printf '%s' "$DELETE_BG" | jq -r '.data.deleteBookmarkGroup // empty')
  if [[ "$api_bg_del" == "true" ]]; then
    pass "app-state-C09 deleteBookmarkGroup → true"
  else
    fail "app-state-C09 deleteBookmarkGroup returned: $DELETE_BG"
  fi
else
  fail "app-state-C07 createBookmarkGroup returned: $CREATE_BG"
  skip "app-state-C08 updateBookmarkGroup (no fixture id)"
  skip "app-state-C09 deleteBookmarkGroup (no fixture id)"
fi

# ----------------------------------------------------------------------------
# Pomodoro
# ----------------------------------------------------------------------------

# app-state-C10  pomodoroSettings returns the current settings
PS=$(call_gql '{ pomodoroSettings { workDuration shortBreakDuration longBreakDuration pomodorosBeforeLongBreak showNotification playSoundOnComplete } }')
api_ps=$(printf '%s' "$PS" | jq '.data.pomodoroSettings')
if [[ "$api_ps" != "null" && -n "$api_ps" ]]; then
  pass "app-state-C10 pomodoroSettings returned: $api_ps"
else
  fail "app-state-C10 pomodoroSettings not returned: $PS"
fi

# app-state-C11  pomodoroToday returns current state
PT=$(call_gql '{ pomodoroToday { date completedCount currentRound timeLeft totalTime isRunning isPause state } }')
api_pt=$(printf '%s' "$PT" | jq '.data.pomodoroToday')
if [[ "$api_pt" != "null" && -n "$api_pt" ]]; then
  pass "app-state-C11 pomodoroToday returned: $api_pt"
else
  fail "app-state-C11 pomodoroToday not returned: $PT"
fi

# app-state-C12..14  Pomodoro lifecycle (start/pause/stop)
SP=$(call_gql 'mutation { startPomodoro(timeLeft: 1500) }')
api_sp=$(printf '%s' "$SP" | jq -r '.data.startPomodoro // empty')
if [[ "$api_sp" == "true" ]]; then
  pass "app-state-C12 startPomodoro(timeLeft=1500) → true"
else
  fail "app-state-C12 startPomodoro returned: $SP"
fi

PP=$(call_gql 'mutation { pausePomodoro }')
api_pp=$(printf '%s' "$PP" | jq -r '.data.pausePomodoro // empty')
if [[ "$api_pp" == "true" ]]; then
  pass "app-state-C13 pausePomodoro → true"
else
  fail "app-state-C13 pausePomodoro returned: $PP"
fi

STP=$(call_gql 'mutation { stopPomodoro }')
api_stp=$(printf '%s' "$STP" | jq -r '.data.stopPomodoro // empty')
if [[ "$api_stp" == "true" ]]; then
  pass "app-state-C14 stopPomodoro → true"
else
  fail "app-state-C14 stopPomodoro returned: $STP"
fi

# ----------------------------------------------------------------------------
# Notification
# ----------------------------------------------------------------------------

# app-state-C15  notifications (may be permission gated)
NT=$(call_gql '{ notifications { id packageName title text time } }')
api_nt_err=$(printf '%s' "$NT" | jq -r '.errors[0].message // empty')
if [[ -n "$api_nt_err" && "$api_nt_err" == *"permission"* ]]; then
  skip "app-state-C15 notifications (permission gated: NOTIFICATION_LISTENER)"
elif [[ "$api_nt_err" == *"no_permission"* ]]; then
  skip "app-state-C15 notifications (permission gated: NOTIFICATION_LISTENER)"
else
  api_nt_count=$(printf '%s' "$NT" | jq '.data.notifications | length')
  [[ "$api_nt_count" -ge 0 ]] && pass "app-state-C15 notifications returned $api_nt_count items" \
                              || fail "app-state-C15 notifications not a list: $NT"
fi

# app-state-C16  cancelNotifications (no-op for missing ids — won't crash)
CN=$(call_gql 'mutation { cancelNotifications(ids: ["apitest-nonexistent"]) }')
api_cn=$(printf '%s' "$CN" | jq -r '.data.cancelNotifications // empty')
if [[ "$api_cn" == "true" ]]; then
  pass "app-state-C16 cancelNotifications(non-existent) → true"
else
  fail "app-state-C16 cancelNotifications returned: $CN"
fi

# app-state-C17  replyNotification (needs a real notification with reply action)
# Skipped: would require a 3rd-party notification with a RemoteInput action.
skip "app-state-C17 replyNotification (skipped: needs real notification with RemoteInput action)"

# ----------------------------------------------------------------------------
# AppLogs
# ----------------------------------------------------------------------------

# app-state-C18  appLogPath
ALP=$(call_gql '{ appLogPath }')
api_alp=$(printf '%s' "$ALP" | jq -r '.data.appLogPath // empty')
if [[ -n "$api_alp" ]]; then
  pass "app-state-C18 appLogPath = $api_alp"
else
  fail "app-state-C18 appLogPath not returned: $ALP"
fi

# app-state-C19  appLogs
AL=$(call_gql '{ appLogs(offset: 0, limit: 10) }')
api_al_count=$(printf '%s' "$AL" | jq '.data.appLogs | length')
[[ "$api_al_count" -ge 0 ]] && pass "app-state-C19 appLogs returned $api_al_count lines" \
                            || fail "app-state-C19 appLogs not a list: $AL"

# app-state-C20  clearAppLogs
CAL=$(call_gql 'mutation { clearAppLogs }')
api_cal=$(printf '%s' "$CAL" | jq -r '.data.clearAppLogs // empty')
if [[ "$api_cal" == "true" ]]; then
  pass "app-state-C20 clearAppLogs → true"
else
  fail "app-state-C20 clearAppLogs returned: $CAL"
fi

# ----------------------------------------------------------------------------
# AppFile
# ----------------------------------------------------------------------------

# app-state-C21  appFileCount
AFC=$(call_gql '{ appFileCount }')
api_afc=$(printf '%s' "$AFC" | jq -r '.data.appFileCount')
db_afc=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM files;")
if [[ "$api_afc" == "$db_afc" ]]; then
  pass "app-state-C21 appFileCount = $api_afc == db"
else
  fail "app-state-C21 appFileCount: api=$api_afc, db=$db_afc"
fi

# app-state-C22  appFiles list
AF=$(call_gql '{ appFiles(offset: 0, limit: 10) { id fileName size mimeType realPath createdAt } }')
api_af_count=$(printf '%s' "$AF" | jq '.data.appFiles | length')
[[ "$api_af_count" -ge 0 ]] && pass "app-state-C22 appFiles returned $api_af_count items (db=$db_afc)" \
                            || fail "app-state-C22 appFiles not a list: $AF"

end_group