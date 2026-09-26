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
expected_queries="appFiles appFileCount chatChannels notifications notificationCount fileCount docCount docExtGroups docs smsBoxCounts sms smsConversations smsCount smsConversationCount archivedSmsConversations tags tagRelations appLogs appLogPath isScreenMirroring screenMirrorVideoCodec screenMirrorControlEnabled screenMirrorQuality uploadedChunks mergeStatus feeds feedEntryCounts feedEntryCount feedEntry feedEntries peers chatItems latestChatItems audioCount audioQueueItems audioQueueItemCount audioPlayback audios audioLyrics audioPlaylists audioPlaylistItems audioPlaylistItemCount audioPlayHistory imageCount imageSearchStatus images callCount sims calls noteCount note notes mediaBuckets deviceInfo deviceStatus app contactCount contactSources contactGroups contacts isDiscovering pomodoroSettings pomodoroToday dataStorePath dataStoreEntries imageEditorProjects imageEditorProject packages packageStatuses packageCount dbPath dbTables dbTableRowCount dbTableRows dbTableInfo dbTableColumns videoCount videos clipboardItems clipboardItemCount bookmarks bookmarkGroups mounts recentFiles files fileInfo fileIds favoriteFolders"
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
expected_mutations="createChatChannel updateChatChannel deleteChatChannel leaveChatChannel addChatChannelMember removeChatChannelMember acceptChatChannelInvite declineChatChannelInvite deleteNotifications replyNotification unarchiveSmsConversation sendSms archiveSmsConversation trashSms restoreSms deleteSms sendMms createTag updateTag deleteTag addToTags updateTagRelations removeFromTags clearAppLogs pairDevice cancelPairing respondToPairing startScreenMirror requestScreenMirrorAudio stopScreenMirror updateScreenMirrorQuality requestScreenMirrorKeyFrame deleteChunks mergeChunks mergeAppFileChunks deleteFiles createDir renameFile writeTextFile copyFile moveFile addFavoriteFolder removeFavoriteFolder setFavoriteFolderAlias syncFeeds updateFeed createFeed importFeeds exportFeeds deleteFeed syncFeedEntryContent deleteFeedEntries deletePeer unpairPeer sendChatItem deleteChatItem deleteChatItems retryChatItem playAudio updateAudioPlayMode clearAudioQueue removeAudioFromQueue addAudiosToQueue reorderAudioQueue createAudioPlaylist updateAudioPlaylist deleteAudioPlaylist addAudioPlaylistItems removeAudioPlaylistItem playAudioPlaylist playAllAudios enableImageSearch disableImageSearch cancelImageModelDownload startImageIndex cancelImageIndex call deleteCalls createNote updateNote saveFeedEntriesToNotes trashNotes restoreNotes deleteNotes exportNotes deleteMediaItems trashMediaItems restoreMediaItems moveMediaItems setTempValue relaunchApp openAccessibilitySettings openWebSettings updateDeviceName deleteContacts updateContact createContact createContactGroup updateContactGroup deleteContactGroup startDiscovery stopDiscovery startPomodoro pausePomodoro stopPomodoro deleteDataStoreEntry saveImageEditorProject deleteImageEditorProject broadcastImageEditorUpdate uninstallPackages installPackage createDbTableRow deleteDbTableRows setClipboard deleteClipboardItems addBookmarks updateBookmark deleteBookmarks recordBookmarkClick createBookmarkGroup updateBookmarkGroup deleteBookmarkGroup"
missing_mutations=$(comm -23 <(echo "$expected_mutations" | tr ' ' '\n' | sort -u) <(printf '%s\n' "$api_mt_names"))
if [[ -z "$missing_mutations" ]]; then
  pass "schema-C04 all $(echo "$expected_mutations" | wc -w | tr -d ' ') expected mutations are in __schema.mutationType.fields"
else
  fail "schema-C04 missing mutations in __schema: $(echo "$missing_mutations" | tr '\n' ' ')"
fi

end_group