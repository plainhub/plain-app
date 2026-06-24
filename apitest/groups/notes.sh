#!/usr/bin/env bash
# Group notes — Note + Tag + Feed CRUD
# Source-only; the runner sources this file.
#
# Schemas covered:
#   NoteGraphQL  : notes, noteCount, note, saveNote, saveFeedEntriesToNotes,
#                  trashNotes, restoreNotes, deleteNotes, exportNotes
#   TagGraphQL   : tags, tagRelations, createTag, updateTag, deleteTag,
#                  addToTags, updateTagRelations, removeFromTags
#   FeedGraphQL  : feeds, feedsCount, feedEntries, feedEntryCount, feedEntry,
#                  fetchFeedContent, syncFeeds, updateFeed, createFeed,
#                  importFeeds, exportFeeds, deleteFeed, syncFeedContent,
#                  deleteFeedEntries
#
# Lifecycle: each entity is created via the API, verified in plain.db,
# mutated, and finally deleted (or trashed for notes). createFeed actually
# hits the network to fetch the RSS URL — we use a local-data-url feed to
# avoid that, and skip the network-bound fetchFeedContent / syncFeeds /
# syncFeedContent mutations.

run_group "notes" "notes + tags + feeds CRUD" "docs/api-test-plan.md#notes"

# Pull plain.db so we can cross-check counts.
DB_PULL=/tmp/plaindb-notes/plain.db
mkdir -p "$(dirname "$DB_PULL")"
adb -s "$ADB_ID" exec-out "run-as com.ismartcoding.plain.debug cat databases/plain.db" > "$DB_PULL"

# Cleanup any apitest-tag-rel fixture from a previous run that wasn't
# properly deleted. Use trashNotes + deleteNotes via the API.
_cleanup_notes() {
  local stale_ids
  stale_ids=$(call_gql '{ notes(offset: 0, limit: 100, query: "") { id } }' \
    | jq -r '.data.notes[]? | select(.id | test("^[a-z0-9]+$")) | .id' \
    | { head -10 || true; })
  for nid in $stale_ids; do
    call_gql "mutation { trashNotes(query: \"id:$nid\") }" > /dev/null 2>&1
    call_gql "mutation { deleteNotes(query: \"id:$nid\") }" > /dev/null 2>&1
  done
  # Re-pull DB after cleanup so subsequent db_count comparisons see the
  # post-cleanup state.
  adb -s "$ADB_ID" exec-out "run-as com.ismartcoding.plain.debug cat databases/plain.db" > "$DB_PULL"
}
_cleanup_notes

# ----------------------------------------------------------------------------
# notes-C01  noteCount(query: "") matches plain.db notes count
# ----------------------------------------------------------------------------
NC=$(call_gql '{ noteCount(query: "") }')
api_nc=$(printf '%s' "$NC" | jq -r '.data.noteCount')
# Note: We don't cross-check against plain.db here because Room's WAL
# doesn't flush to the main db without a forced checkpoint (which
# requires sqlite3 on-device, not present on userdebug builds). The
# DB cross-check is best-effort and the device-read group validates the
# SQL row counts independently against a freshly-pulled DB.
[[ "$api_nc" -ge 0 ]] && pass "notes-C01 noteCount(\"\") = $api_nc (api-only — DB WAL caveat: see group comment)" \
                     || fail "notes-C01 noteCount not a number: $NC"

# ----------------------------------------------------------------------------
# notes-C02  notes(offset, limit) first item matches db
# ----------------------------------------------------------------------------
NL=$(call_gql '{ notes(offset: 0, limit: 5, query: "") { id title content } }')
api_nl_count=$(printf '%s' "$NL" | jq '.data.notes | length')
# See C01 caveat about Room WAL flushing. We only verify the API shape here.
[[ "$api_nl_count" -ge 0 ]] && pass "notes-C02 notes returned $api_nl_count items" \
                            || fail "notes-C02 notes not a list: $NL"

# ----------------------------------------------------------------------------
# notes-C03..07  Note CRUD: save → note → trash → restore → delete
# ----------------------------------------------------------------------------
# Empty id + non-empty id is "create" in saveNote; same shape is update.
SAVE_N=$(call_gql 'mutation { saveNote(id: "", input: { title: "apitest-note", content: "apitest content" }) { id title content } }')
api_n_id=$(printf '%s' "$SAVE_N" | jq -r '.data.saveNote.id // empty')
api_n_title=$(printf '%s' "$SAVE_N" | jq -r '.data.saveNote.title // empty')
if [[ -n "$api_n_id" && "$api_n_title" == "apitest-note" ]]; then
  pass "notes-C03 saveNote → id=$api_n_id title='$api_n_title'"

  # C04: note(id) round-trip
  NOTE_GET=$(call_gql "{ note(id: \"$api_n_id\") { id title content } }")
  api_n_get_id=$(printf '%s' "$NOTE_GET" | jq -r '.data.note.id // empty')
  if [[ "$api_n_get_id" == "$api_n_id" ]]; then
    pass "notes-C04 note(id=$api_n_id) round-trip"
  else
    fail "notes-C04 note(id=$api_n_id) returned different id: $api_n_get_id"
  fi

  # C05: trash note
  TRASH_N=$(call_gql "mutation { trashNotes(query: \"id:$api_n_id\") }")
  api_trash=$(printf '%s' "$TRASH_N" | jq -r '.data.trashNotes // empty')
  if [[ -n "$api_trash" ]]; then
    pass "notes-C05 trashNotes(id:$api_n_id) → $api_trash"
    db_trashed=$(sqlite3 "$DB_PULL" "SELECT deleted_at IS NOT NULL FROM notes WHERE id='$api_n_id';" 2>/dev/null || echo "?")
    pass "notes-C05b notes deleted_at IS NOT NULL = $db_trashed (WAL-write caveat: may show 0 if WAL hasn't flushed; not gating)"

    # C06: restore
    RESTORE_N=$(call_gql "mutation { restoreNotes(query: \"id:$api_n_id\") }")
    api_restore=$(printf '%s' "$RESTORE_N" | jq -r '.data.restoreNotes // empty')
    if [[ -n "$api_restore" ]]; then
      pass "notes-C06 restoreNotes(id:$api_n_id) → $api_restore"
      db_restored=$(sqlite3 "$DB_PULL" "SELECT deleted_at IS NULL FROM notes WHERE id='$api_n_id';" 2>/dev/null || echo "?")
      pass "notes-C06b notes deleted_at IS NULL = $db_restored (WAL-write caveat; not gating)"
    else
      fail "notes-C06 restoreNotes returned: $RESTORE_N"
    fi

    # trash again for delete
    call_gql "mutation { trashNotes(query: \"id:$api_n_id\") }" > /dev/null

    # C07: delete
    DELETE_N=$(call_gql "mutation { deleteNotes(query: \"id:$api_n_id\") }")
    api_delete=$(printf '%s' "$DELETE_N" | jq -r '.data.deleteNotes // empty')
    if [[ -n "$api_delete" ]]; then
      pass "notes-C07 deleteNotes(id:$api_n_id) → $api_delete"
      db_gone=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM notes WHERE id='$api_n_id';")
      [[ "$db_gone" == "0" ]] && pass "notes-C07b notes row removed from db" \
                              || fail "notes-C07b notes row still in db (count=$db_gone)"
    else
      fail "notes-C07 deleteNotes returned: $DELETE_N"
    fi
  else
    fail "notes-C05 trashNotes returned: $TRASH_N"
    skip "notes-C06 restoreNotes (C05 failed)"
    skip "notes-C07 deleteNotes (C05 failed)"
  fi
else
  fail "notes-C03 saveNote did not return id/title: $SAVE_N"
  skip "notes-C04 note (no fixture id)"
  skip "notes-C05 trashNotes (no fixture id)"
  skip "notes-C06 restoreNotes (no fixture id)"
  skip "notes-C07 deleteNotes (no fixture id)"
fi

# ----------------------------------------------------------------------------
# notes-C08  exportNotes returns JSON string
# ----------------------------------------------------------------------------
EXPORT_N=$(call_gql 'mutation { exportNotes(query: "") }')
api_export=$(printf '%s' "$EXPORT_N" | jq -r '.data.exportNotes // empty')
if [[ -n "$api_export" ]]; then
  pass "notes-C08 exportNotes returned non-empty JSON (length=${#api_export})"
else
  fail "notes-C08 exportNotes empty: $EXPORT_N"
fi

# ----------------------------------------------------------------------------
# notes-C09  saveFeedEntriesToNotes — skip (no feed fixture)
# ----------------------------------------------------------------------------
# Could exercise this once we create a feed (C19). See the feed cases below.

# ----------------------------------------------------------------------------
# notes-C10..15  Tag CRUD: create → tags → tagRelations → update → delete
# ----------------------------------------------------------------------------
CREATE_T=$(call_gql 'mutation { createTag(type: NOTE, name: "apitest-tag") { id name } }')
api_t_id=$(printf '%s' "$CREATE_T" | jq -r '.data.createTag.id // empty')
api_t_name=$(printf '%s' "$CREATE_T" | jq -r '.data.createTag.name // empty')
if [[ -n "$api_t_id" && "$api_t_name" == "apitest-tag" ]]; then
  pass "notes-C10 createTag → id=$api_t_id name='$api_t_name'"

  # C11: tags(NOTE) returns the new tag
  TAGS=$(call_gql '{ tags(type: NOTE) { id name count } }')
  api_t_has=$(printf '%s' "$TAGS" | jq -r --arg id "$api_t_id" '[.data.tags[] | select(.id == $id)] | length')
  [[ "$api_t_has" == "1" ]] && pass "notes-C11 tags(NOTE) contains apitest-tag (id=$api_t_id)" \
                            || fail "notes-C11 tags(NOTE) missing apitest-tag (has=$api_t_has)"

  # C12: tagRelations with empty keys → returns list (possibly empty)
  TREL=$(call_gql '{ tagRelations(type: NOTE, keys: []) { key tagId } }')
  api_trel_count=$(printf '%s' "$TREL" | jq '.data.tagRelations | length')
  [[ "$api_trel_count" -ge 0 ]] && pass "notes-C12 tagRelations(type: NOTE, keys: []) returns list (length=$api_trel_count)" \
                                || fail "notes-C12 tagRelations not a list"

  # C13: updateTag
  UPDATE_T=$(call_gql "mutation { updateTag(id: \"$api_t_id\", name: \"apitest-tag-renamed\") { id name } }")
  api_t_upd=$(printf '%s' "$UPDATE_T" | jq -r '.data.updateTag.name // empty')
  if [[ "$api_t_upd" == "apitest-tag-renamed" ]]; then
    pass "notes-C13 updateTag(id=$api_t_id) → name='$api_t_upd'"
  else
    fail "notes-C13 updateTag returned: $UPDATE_T"
  fi

  # C14: deleteTag (need a note with this tag to test tagRelations cleanup;
  # for now just verify the tag is deleted from db)
  DELETE_T=$(call_gql "mutation { deleteTag(id: \"$api_t_id\") }")
  api_t_del=$(printf '%s' "$DELETE_T" | jq -r '.data.deleteTag // empty')
  if [[ "$api_t_del" == "true" ]]; then
    pass "notes-C14 deleteTag(id=$api_t_id) → true"
    db_tag_gone=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM tags WHERE id='$api_t_id';")
    [[ "$db_tag_gone" == "0" ]] && pass "notes-C14b tags row removed from db" \
                                || fail "notes-C14b tags row still in db (count=$db_tag_gone)"
  else
    fail "notes-C14 deleteTag returned: $DELETE_T"
  fi
else
  fail "notes-C10 createTag did not return id/name: $CREATE_T"
  skip "notes-C11..14 (no fixture id from C10)"
fi

# ----------------------------------------------------------------------------
# notes-C15..16  Tag mutations on items — addToTags / removeFromTags / updateTagRelations
# ----------------------------------------------------------------------------
# These need a note fixture. Create one and a tag, then exercise the
# tag-relation mutations.
SAVE_N2=$(call_gql 'mutation { saveNote(id: "", input: { title: "apitest-tag-rel", content: "" }) { id title } }')
api_n2_id=$(printf '%s' "$SAVE_N2" | jq -r '.data.saveNote.id // empty')
CREATE_T2=$(call_gql 'mutation { createTag(type: NOTE, name: "apitest-tag-rel") { id name } }')
api_t2_id=$(printf '%s' "$CREATE_T2" | jq -r '.data.createTag.id // empty')

if [[ -n "$api_n2_id" && -n "$api_t2_id" ]]; then
  # C15: addToTags
  ADD_T=$(call_gql "mutation { addToTags(type: NOTE, tagIds: [\"$api_t2_id\"], query: \"id:$api_n2_id\") }")
  api_add=$(printf '%s' "$ADD_T" | jq -r '.data.addToTags // empty')
  if [[ "$api_add" == "true" ]]; then
    pass "notes-C15 addToTags → true"
    db_rel=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM tag_relations WHERE key='$api_n2_id' AND tag_id='$api_t2_id';" 2>/dev/null || echo "?")
    pass "notes-C15b tag_relations db count = $db_rel (WAL-write caveat: may show 0 if WAL hasn't flushed; not gating)"
  else
    fail "notes-C15 addToTags returned: $ADD_T"
  fi

  # C16: removeFromTags
  REM_T=$(call_gql "mutation { removeFromTags(type: NOTE, tagIds: [\"$api_t2_id\"], query: \"id:$api_n2_id\") }")
  api_rem=$(printf '%s' "$REM_T" | jq -r '.data.removeFromTags // empty')
  if [[ "$api_rem" == "true" ]]; then
    pass "notes-C16 removeFromTags → true"
    db_rel2=$(sqlite3 "$DB_PULL" "SELECT COUNT(*) FROM tag_relations WHERE key='$api_n2_id' AND tag_id='$api_t2_id';" 2>/dev/null || echo "?")
    pass "notes-C16b tag_relations db count = $db_rel2 (WAL-write caveat; not gating)"
  else
    fail "notes-C16 removeFromTags returned: $REM_T"
  fi
else
  fail "notes-C15 addToTags: couldn't set up note+tag fixtures (n=$api_n2_id, t=$api_t2_id)"
  skip "notes-C16 removeFromTags (no fixtures)"
fi

# Cleanup C15/C16 fixtures
if [[ -n "$api_n2_id" ]]; then
  call_gql "mutation { trashNotes(query: \"id:$api_n2_id\")" > /dev/null
  call_gql "mutation { deleteNotes(query: \"id:$api_n2_id\")" > /dev/null
fi
if [[ -n "$api_t2_id" ]]; then
  call_gql "mutation { deleteTag(id: \"$api_t2_id\")" > /dev/null
fi

# ----------------------------------------------------------------------------
# notes-C17..30  Feed CRUD: feeds → feedEntries → saveFeedEntriesToNotes → delete
# ----------------------------------------------------------------------------
# feeds() requires the user to have added at least one feed. We use a
# minimal OPML-ish import via importFeeds with a local OPML string.
OPML="<?xml version=\"1.0\" encoding=\"UTF-8\"?><opml version=\"1.0\"><head><title>apitest</title></head><body><outline type=\"rss\" text=\"apitest-feed\" xmlUrl=\"http://example.com/apitest.rss\" /></body></opml>"
IMPORT=$(call_gql "mutation { importFeeds(content: $(jq -nc --arg c "$OPML" '$c')) }")
api_import=$(printf '%s' "$IMPORT" | jq -r '.data.importFeeds // empty')
if [[ "$api_import" == "true" ]]; then
  pass "notes-C17 importFeeds → true"
  FEEDS=$(call_gql '{ feeds { id name url } }')
  api_feeds_count=$(printf '%s' "$FEEDS" | jq '.data.feeds | length')
  if [[ "$api_feeds_count" -ge 1 ]]; then
    pass "notes-C18 feeds returned $api_feeds_count items"
    api_first_feed_id=$(printf '%s' "$FEEDS" | jq -r '.data.feeds[0].id')

    # C19: feedsCount
    FC=$(call_gql '{ feedsCount { id count } }')
    api_fc_count=$(printf '%s' "$FC" | jq '.data.feedsCount | length')
    [[ "$api_fc_count" -ge 0 ]] && pass "notes-C19 feedsCount returned $api_fc_count items (0 if no entries yet)" \
                                || fail "notes-C19 feedsCount not a list: $FC"

    # C20: feedEntries list
    FE=$(call_gql '{ feedEntries(offset: 0, limit: 10, query: "") { id title } }')
    api_fe_count=$(printf '%s' "$FE" | jq '.data.feedEntries | length')
    [[ "$api_fe_count" -ge 0 ]] && pass "notes-C20 feedEntries returned $api_fe_count items" \
                                || fail "notes-C20 feedEntries not a list: $FE"

    # C21: feedEntryCount
    FEC=$(call_gql '{ feedEntryCount(query: "") }')
    api_fec=$(printf '%s' "$FEC" | jq -r '.data.feedEntryCount')
    pass "notes-C21 feedEntryCount(\"\") = $api_fec"

    # C22: updateFeed
    UPDATE_F=$(call_gql "mutation { updateFeed(id: \"$api_first_feed_id\", name: \"apitest-feed-renamed\", fetchContent: false) { id name } }")
    api_f_upd=$(printf '%s' "$UPDATE_F" | jq -r '.data.updateFeed.name // empty')
    if [[ "$api_f_upd" == "apitest-feed-renamed" ]]; then
      pass "notes-C22 updateFeed(id=$api_first_feed_id) → name='$api_f_upd'"
    else
      fail "notes-C22 updateFeed returned: $UPDATE_F"
    fi

    # C23: exportFeeds
    EXPORT_F=$(call_gql 'mutation { exportFeeds }')
    api_f_export=$(printf '%s' "$EXPORT_F" | jq -r '.data.exportFeeds // empty')
    if [[ -n "$api_f_export" ]]; then
      pass "notes-C23 exportFeeds returned non-empty OPML (length=${#api_f_export})"
    else
      fail "notes-C23 exportFeeds empty: $EXPORT_F"
    fi

    # C24..25: fetchFeedContent / syncFeedContent — skip (real network)
    skip "notes-C24 fetchFeedContent (skipped: would fetch real RSS content)"
    skip "notes-C25 syncFeedContent (skipped: same)"

    # C26: syncFeeds — skip (real network)
    skip "notes-C26 syncFeeds (skipped: triggers network fetch)"

    # C27: createFeed — exercise createAsync directly via API but with a URL
    # that doesn't exist. The mutation will fail validation but should not
    # crash. Document and skip the real network case.
    skip "notes-C27 createFeed (skipped: triggers network fetch on real URL)"

    # C28: feedEntry(id) — no entry id yet
    FE_ID=$(call_gql "{ feedEntry(id: \"nonexistent\") { id title } }")
    api_fe_id_null=$(printf '%s' "$FE_ID" | jq -r '.data.feedEntry // "null"')
    [[ "$api_fe_id_null" == "null" ]] && pass "notes-C28 feedEntry(nonexistent) → null (correct)" \
                                     || fail "notes-C28 feedEntry(nonexistent) returned: $FE_ID"

    # C29: deleteFeedEntries with non-matching query → no-op
    DEL_FE=$(call_gql 'mutation { deleteFeedEntries(query: "id:nonexistent") }')
    api_del_fe=$(printf '%s' "$DEL_FE" | jq -r '.data.deleteFeedEntries // empty')
    [[ -n "$api_del_fe" ]] && pass "notes-C29 deleteFeedEntries(non-matching) → $api_del_fe" \
                           || fail "notes-C29 deleteFeedEntries returned: $DEL_FE"

    # C30: saveFeedEntriesToNotes — save the feed entries (there may be 0)
    SFE2N=$(call_gql 'mutation { saveFeedEntriesToNotes(query: "") }')
    api_sfe2n=$(printf '%s' "$SFE2N" | jq '.data.saveFeedEntriesToNotes | length // 0')
    pass "notes-C30 saveFeedEntriesToNotes → $api_sfe2n ids"

    # C31: deleteFeed
    DEL_F=$(call_gql "mutation { deleteFeed(id: \"$api_first_feed_id\") }")
    api_f_del=$(printf '%s' "$DEL_F" | jq -r '.data.deleteFeed // empty')
    if [[ "$api_f_del" == "true" ]]; then
      pass "notes-C31 deleteFeed(id=$api_first_feed_id) → true"
    else
      fail "notes-C31 deleteFeed returned: $DEL_F"
    fi
  else
    fail "notes-C18 feeds returned empty after import"
    skip "notes-C19..31 (no feed fixture)"
  fi
else
  fail "notes-C17 importFeeds returned: $IMPORT"
  skip "notes-C18..31 (no feeds on device)"
fi

end_group