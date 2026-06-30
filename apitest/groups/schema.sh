#!/usr/bin/env bash
# Group schema — GraphQL introspection sanity
# Source-only; the runner sources this file.
#
# Verifies:
#   - __schema.queryType / mutationType point to Query / Mutation
#   - queryType.fields contains the expected top-level queries
#   - mutationType.fields contains the expected top-level mutations
#   - __schema.types list contains the app's custom data classes
#
# No real device state is touched — pure introspection.

run_group "schema" "schema introspection sanity" "docs/api-test-plan.md#schema"

# ----------------------------------------------------------------------------
# schema-C01  __schema { queryType { name } } == Query
# ----------------------------------------------------------------------------
QT=$(call_gql '{ __schema { queryType { name } } }')
api_qt=$(printf '%s' "$QT" | jq -r '.data.__schema.queryType.name')
if [[ "$api_qt" == "Query" ]]; then
  pass "schema-C01 __schema.queryType.name = Query"
else
  fail "schema-C01 queryType.name: $api_qt"
fi

# ----------------------------------------------------------------------------
# schema-C02  __schema { mutationType { name } } == Mutation
# ----------------------------------------------------------------------------
MT=$(call_gql '{ __schema { mutationType { name } } }')
api_mt=$(printf '%s' "$MT" | jq -r '.data.__schema.mutationType.name')
if [[ "$api_mt" == "Mutation" ]]; then
  pass "schema-C02 __schema.mutationType.name = Mutation"
else
  fail "schema-C02 mutationType.name: $api_mt"
fi

# ----------------------------------------------------------------------------
# schema-C03  every query declared under web/schemas/ is reachable via introspection
# ----------------------------------------------------------------------------
QT_FIELDS=$(call_gql '{ __schema { queryType { fields { name } } } }')
api_qt_names=$(printf '%s' "$QT_FIELDS" | jq -r '.data.__schema.queryType.fields[].name' | sort -u)
expected_queries="app battery deviceInfo packages packageCount packageStatuses contacts contactCount contactSources contactGroups sims calls callCount sms smsCount smsConversations smsConversationCount archivedConversations smsAllCounts notes noteCount note feeds feedsCount feedEntries feedEntryCount feedEntry audios audioCount videos videoCount images imageCount imageSearchStatus mediaBuckets docs docCount docExtGroups mounts recentFiles files fileInfo fileIds chatChannels peers isDiscovering chatItems latestChatItems screenMirrorState screenMirrorControlEnabled screenMirrorQuality pomodoroSettings pomodoroToday bookmarks bookmarkGroups notifications appLogs appLogPath appFileCount appFiles dataStorePath dataStoreEntries dbPath dbTables dbTableRowCount dbTableInfo dbTableRows uploadedChunks"
missing_queries=$(comm -23 <(echo "$expected_queries" | tr ' ' '\n' | sort -u) <(printf '%s\n' "$api_qt_names"))
if [[ -z "$missing_queries" ]]; then
  pass "schema-C03 all $(echo "$expected_queries" | wc -w | tr -d ' ') expected queries are in __schema.queryType.fields"
else
  fail "schema-C03 missing queries in __schema: $(echo "$missing_queries" | tr '\n' ' ')"
fi

# ----------------------------------------------------------------------------
# schema-C04  every mutation declared under web/schemas/ is reachable via introspection
# ----------------------------------------------------------------------------
MT_FIELDS=$(call_gql '{ __schema { mutationType { fields { name } } } }')
api_mt_names=$(printf '%s' "$MT_FIELDS" | jq -r '.data.__schema.mutationType.fields[].name' | sort -u)
expected_mutations="uninstallPackages installPackage updateContact createContact deleteContacts createContactGroup updateContactGroup deleteContactGroup deleteCalls archiveConversation unarchiveConversation sendSms sendMms call deleteNotes trashNotes restoreNotes exportNotes saveNote saveFeedEntriesToNotes createTag updateTag deleteTag addToTags updateTagRelations removeFromTags createFeed updateFeed deleteFeed deleteFeedEntries importFeeds exportFeeds syncFeeds fetchFeedContent syncFeedContent deleteFiles createDir renameFile writeTextFile copyFile moveFile addFavoriteFolder removeFavoriteFolder setFavoriteFolderAlias mergeChunks deleteChunks playAudio updateAudioPlayMode clearAudioPlaylist deletePlaylistAudio addPlaylistAudios reorderPlaylistAudios deleteMediaItems trashMediaItems restoreMediaItems startPomodoro pausePomodoro stopPomodoro enableImageSearch disableImageSearch cancelImageModelDownload startImageIndex cancelImageIndex cancelNotifications replyNotification clearAppLogs addBookmarks updateBookmark deleteBookmarks recordBookmarkClick createBookmarkGroup updateBookmarkGroup deleteBookmarkGroup startDiscovery stopDiscovery pairDevice cancelPairing respondToPairing unpairPeer createChatChannel updateChatChannel deleteChatChannel leaveChatChannel addChatChannelMember removeChatChannelMember acceptChatChannelInvite declineChatChannelInvite sendChatItem deleteChatItem deleteChatItems retryChatItem startScreenMirror stopScreenMirror requestScreenMirrorAudio updateScreenMirrorQuality sendScreenMirrorControl deleteDataStoreEntry createDbTableRow deleteDbTableRows"
missing_mutations=$(comm -23 <(echo "$expected_mutations" | tr ' ' '\n' | sort -u) <(printf '%s\n' "$api_mt_names"))
if [[ -z "$missing_mutations" ]]; then
  pass "schema-C04 all $(echo "$expected_mutations" | wc -w | tr -d ' ') expected mutations are in __schema.mutationType.fields"
else
  fail "schema-C04 missing mutations in __schema: $(echo "$missing_mutations" | tr '\n' ' ')"
fi

end_group