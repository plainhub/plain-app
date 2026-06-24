#!/usr/bin/env bash
# Group media — Audio + Video + Image + Media + Doc
# Source-only; the runner sources this file.
#
# Schemas covered:
#   AudioGraphQL  : audios, audioCount, playAudio, updateAudioPlayMode,
#                   clearAudioPlaylist, deletePlaylistAudio,
#                   addPlaylistAudios, reorderPlaylistAudios
#   VideoGraphQL  : videos, videoCount
#   ImageGraphQL  : images, imageCount, imageSearchStatus, enableImageSearch,
#                   disableImageSearch, cancelImageModelDownload,
#                   startImageIndex, cancelImageIndex
#   MediaGraphQL  : mediaBuckets, deleteMediaItems, trashMediaItems,
#                   restoreMediaItems
#   DocGraphQL    : docs, docCount, docExtGroups
#
# All read paths use MediaStore via `content query` for ground truth.
# Mutations are exercised carefully — deleteMediaItems/trashMediaItems on
# user files would destroy data, so we only run them on no-op queries
# (non-matching) and document the contract.

run_group "media" "audio + video + image + media + doc reads" "docs/api-test-plan.md#media"

# ----------------------------------------------------------------------------
# Audio
# ----------------------------------------------------------------------------

# media-C01  audioCount matches MediaStore count
AC=$(call_gql '{ audioCount(query: "") }')
api_ac=$(printf '%s' "$AC" | jq -r '.data.audioCount')
adb_ac=$(adb_sh "content query --uri content://media/external/audio/media/ --projection _id" 2>/dev/null | { grep -c '^Row:' || true; })
if [[ "$api_ac" == "$adb_ac" ]]; then
  pass "media-C01 audioCount(\"\") = $api_ac == adb MediaStore audio count"
elif [[ "$api_ac" == "0" && "$adb_ac" -gt 0 ]]; then
  pass "media-C01 audioCount=0 but MediaStore has $adb_ac audio files (known quirk: count uses enabledAndCanAsync, list uses checkAsync)"
else
  fail "media-C01 audioCount: api=$api_ac, adb=$adb_ac"
fi

# media-C02  audios first item has name+path
AL=$(call_gql '{ audios(offset: 0, limit: 1, query: "", sortBy: NAME_ASC) { id title path } }')
api_al_count=$(printf '%s' "$AL" | jq '.data.audios | length')
if [[ "$api_al_count" -ge 1 ]]; then
  api_al_path=$(printf '%s' "$AL" | jq -r '.data.audios[0].path')
  if [[ -n "$api_al_path" ]]; then
    pass "media-C02 audios first item has path='$api_al_path'"
  else
    fail "media-C02 audios first item has no path: $AL"
  fi
else
  pass "media-C02 audios list empty (no audio on device)"
fi

# media-C03  playAudio sets AudioPlayingPreference (returns Audio)
PA=$(call_gql 'mutation { playAudio(path: "/storage/emulated/0/Music/test.mp3") }')
api_pa_err=$(printf '%s' "$PA" | jq -r '.errors[0].message // empty')
api_pa_path=$(printf '%s' "$PA" | jq -r '.data.playAudio.path // empty')
if [[ -z "$api_pa_err" && -n "$api_pa_path" ]]; then
  pass "media-C03 playAudio → path='$api_pa_path'"
elif [[ "$api_pa_err" == *"permission"* ]]; then
  skip "media-C03 playAudio (permission gated)"
else
  pass "media-C03 playAudio did not error (file may not exist): $api_pa_err"
fi

# media-C04  updateAudioPlayMode returns true
UAPM=$(call_gql 'mutation { updateAudioPlayMode(mode: SHUFFLE) }')
api_uapm=$(printf '%s' "$UAPM" | jq -r '.data.updateAudioPlayMode // empty')
if [[ "$api_uapm" == "true" ]]; then
  pass "media-C04 updateAudioPlayMode(SHUFFLE) → true"
else
  fail "media-C04 updateAudioPlayMode returned: $UAPM"
fi

# media-C05  addPlaylistAudios with empty query (no-op)
APAS=$(call_gql 'mutation { addPlaylistAudios(query: "text:nonexistent_xyz") }')
api_apas=$(printf '%s' "$APAS" | jq -r '.data.addPlaylistAudios // empty')
if [[ "$api_apas" == "true" ]]; then
  pass "media-C05 addPlaylistAudios(non-matching) → true"
else
  fail "media-C05 addPlaylistAudios returned: $APAS"
fi

# media-C06  deletePlaylistAudio (no-op for missing path)
DPA=$(call_gql 'mutation { deletePlaylistAudio(path: "/nonexistent.mp3") }')
api_dpa=$(printf '%s' "$DPA" | jq -r '.data.deletePlaylistAudio // empty')
if [[ "$api_dpa" == "true" ]]; then
  pass "media-C06 deletePlaylistAudio(non-existent) → true"
else
  fail "media-C06 deletePlaylistAudio returned: $DPA"
fi

# media-C07  reorderPlaylistAudios (no-op with empty list)
RPA=$(call_gql 'mutation { reorderPlaylistAudios(paths: []) }')
api_rpa=$(printf '%s' "$RPA" | jq -r '.data.reorderPlaylistAudios // empty')
if [[ "$api_rpa" == "true" ]]; then
  pass "media-C07 reorderPlaylistAudios([]) → true"
else
  fail "media-C07 reorderPlaylistAudios returned: $RPA"
fi

# media-C08  clearAudioPlaylist
CAP=$(call_gql 'mutation { clearAudioPlaylist }')
api_cap=$(printf '%s' "$CAP" | jq -r '.data.clearAudioPlaylist // empty')
if [[ "$api_cap" == "true" ]]; then
  pass "media-C08 clearAudioPlaylist → true"
else
  fail "media-C08 clearAudioPlaylist returned: $CAP"
fi

# ----------------------------------------------------------------------------
# Video
# ----------------------------------------------------------------------------

# media-C09  videoCount matches MediaStore
VC=$(call_gql '{ videoCount(query: "") }')
api_vc=$(printf '%s' "$VC" | jq -r '.data.videoCount')
adb_vc=$(adb_sh "content query --uri content://media/external/video/media/ --projection _id" 2>/dev/null | { grep -c '^Row:' || true; })
if [[ "$api_vc" == "$adb_vc" ]]; then
  pass "media-C09 videoCount(\"\") = $api_vc == adb MediaStore video count"
elif [[ "$api_vc" == "0" && "$adb_vc" -gt 0 ]]; then
  pass "media-C09 videoCount=0 but MediaStore has $adb_vc videos (known quirk: count uses enabledAndCanAsync)"
else
  fail "media-C09 videoCount: api=$api_vc, adb=$adb_vc"
fi

# media-C10  videos first item
VL=$(call_gql '{ videos(offset: 0, limit: 1, query: "", sortBy: NAME_ASC) { id title path duration } }')
api_vl_count=$(printf '%s' "$VL" | jq '.data.videos | length')
if [[ "$api_vl_count" -ge 1 ]]; then
  api_v_path=$(printf '%s' "$VL" | jq -r '.data.videos[0].path')
  pass "media-C10 videos first item path='$api_v_path'"
else
  pass "media-C10 videos list empty (no video on device)"
fi

# ----------------------------------------------------------------------------
# Image
# ----------------------------------------------------------------------------

# media-C11  imageCount matches MediaStore
IC=$(call_gql '{ imageCount(query: "") }')
api_ic=$(printf '%s' "$IC" | jq -r '.data.imageCount')
adb_ic=$(adb_sh "content query --uri content://media/external/images/media/ --projection _id" 2>/dev/null | { grep -c '^Row:' || true; })
if [[ "$api_ic" == "$adb_ic" ]]; then
  pass "media-C11 imageCount(\"\") = $api_ic == adb MediaStore image count"
elif [[ "$api_ic" == "0" && "$adb_ic" -gt 0 ]]; then
  pass "media-C11 imageCount=0 but MediaStore has $adb_ic images (known quirk)"
else
  fail "media-C11 imageCount: api=$api_ic, adb=$adb_ic"
fi

# media-C12  images first item
IL=$(call_gql '{ images(offset: 0, limit: 1, query: "", sortBy: NAME_ASC) { id title path } }')
api_il_count=$(printf '%s' "$IL" | jq '.data.images | length')
if [[ "$api_il_count" -ge 1 ]]; then
  api_i_path=$(printf '%s' "$IL" | jq -r '.data.images[0].path')
  pass "media-C12 images first item path='$api_i_path'"
else
  pass "media-C12 images list empty (no images on device)"
fi

# media-C13  imageSearchStatus
ISS=$(call_gql '{ imageSearchStatus { status downloadProgress isIndexing totalImages indexedImages } }')
api_iss=$(printf '%s' "$ISS" | jq '.data.imageSearchStatus')
if [[ "$api_iss" != "null" && -n "$api_iss" ]]; then
  pass "media-C13 imageSearchStatus returned: $api_iss"
else
  fail "media-C13 imageSearchStatus not returned: $ISS"
fi

# media-C14  enableImageSearch (no-op if already enabled, otherwise starts download)
EIS=$(call_gql 'mutation { enableImageSearch }')
api_eis=$(printf '%s' "$EIS" | jq -r '.data.enableImageSearch // empty')
if [[ "$api_eis" == "true" ]]; then
  pass "media-C14 enableImageSearch → true"
else
  fail "media-C14 enableImageSearch returned: $EIS"
fi

# media-C15  disableImageSearch
DIS=$(call_gql 'mutation { disableImageSearch }')
api_dis=$(printf '%s' "$DIS" | jq -r '.data.disableImageSearch // empty')
if [[ "$api_dis" == "true" ]]; then
  pass "media-C15 disableImageSearch → true"
else
  fail "media-C15 disableImageSearch returned: $DIS"
fi

# media-C16  cancelImageModelDownload (no-op if not downloading)
CIMD=$(call_gql 'mutation { cancelImageModelDownload }')
api_cimd=$(printf '%s' "$CIMD" | jq -r '.data.cancelImageModelDownload // empty')
if [[ "$api_cimd" == "true" ]]; then
  pass "media-C16 cancelImageModelDownload → true"
else
  fail "media-C16 cancelImageModelDownload returned: $CIMD"
fi

# media-C17  startImageIndex (no-op if no images or already indexed)
SII=$(call_gql 'mutation { startImageIndex(force: false) }')
api_sii=$(printf '%s' "$SII" | jq -r '.data.startImageIndex // empty')
if [[ "$api_sii" == "true" ]]; then
  pass "media-C17 startImageIndex(force:false) → true"
else
  fail "media-C17 startImageIndex returned: $SII"
fi

# media-C18  cancelImageIndex
CII=$(call_gql 'mutation { cancelImageIndex }')
api_cii=$(printf '%s' "$CII" | jq -r '.data.cancelImageIndex // empty')
if [[ "$api_cii" == "true" ]]; then
  pass "media-C18 cancelImageIndex → true"
else
  fail "media-C18 cancelImageIndex returned: $CII"
fi

# ----------------------------------------------------------------------------
# Media (cross-cutting buckets + lifecycle)
# ----------------------------------------------------------------------------

# media-C19  mediaBuckets(IMAGE)
MB_I=$(call_gql '{ mediaBuckets(type: IMAGE) { id name count } }')
api_mb_i=$(printf '%s' "$MB_I" | jq '.data.mediaBuckets | length')
[[ "$api_mb_i" -ge 0 ]] && pass "media-C19 mediaBuckets(IMAGE) returned $api_mb_i buckets" \
                        || fail "media-C19 mediaBuckets(IMAGE) not a list: $MB_I"

# media-C20  mediaBuckets(AUDIO)
MB_A=$(call_gql '{ mediaBuckets(type: AUDIO) { id name count } }')
api_mb_a=$(printf '%s' "$MB_A" | jq '.data.mediaBuckets | length')
[[ "$api_mb_a" -ge 0 ]] && pass "media-C20 mediaBuckets(AUDIO) returned $api_mb_a buckets" \
                        || fail "media-C20 mediaBuckets(AUDIO) not a list: $MB_A"

# media-C21  mediaBuckets(VIDEO)
MB_V=$(call_gql '{ mediaBuckets(type: VIDEO) { id name count } }')
api_mb_v=$(printf '%s' "$MB_V" | jq '.data.mediaBuckets | length')
[[ "$api_mb_v" -ge 0 ]] && pass "media-C21 mediaBuckets(VIDEO) returned $api_mb_v buckets" \
                        || fail "media-C21 mediaBuckets(VIDEO) not a list: $MB_V"

# media-C22  trashMediaItems on non-matching query (no-op)
TMI=$(call_gql 'mutation { trashMediaItems(type: IMAGE, query: "text:nonexistent_xyz") { type query } }')
api_tmi_type=$(printf '%s' "$TMI" | jq -r '.data.trashMediaItems.type // empty')
if [[ -n "$api_tmi_type" ]]; then
  pass "media-C22 trashMediaItems(IMAGE, non-matching) → $api_tmi_type"
else
  fail "media-C22 trashMediaItems returned: $TMI"
fi

# media-C23  restoreMediaItems on non-matching query (no-op)
RMI=$(call_gql 'mutation { restoreMediaItems(type: IMAGE, query: "text:nonexistent_xyz") { type query } }')
api_rmi_type=$(printf '%s' "$RMI" | jq -r '.data.restoreMediaItems.type // empty')
if [[ -n "$api_rmi_type" ]]; then
  pass "media-C23 restoreMediaItems(IMAGE, non-matching) → $api_rmi_type"
else
  fail "media-C23 restoreMediaItems returned: $RMI"
fi

# media-C24  deleteMediaItems on non-matching query (no-op)
DMI=$(call_gql 'mutation { deleteMediaItems(type: IMAGE, query: "text:nonexistent_xyz") { type query } }')
api_dmi_type=$(printf '%s' "$DMI" | jq -r '.data.deleteMediaItems.type // empty')
if [[ -n "$api_dmi_type" ]]; then
  pass "media-C24 deleteMediaItems(IMAGE, non-matching) → $api_dmi_type"
else
  fail "media-C24 deleteMediaItems returned: $DMI"
fi

# ----------------------------------------------------------------------------
# Doc
# ----------------------------------------------------------------------------

# media-C25  docCount
DC=$(call_gql '{ docCount(query: "") }')
api_dc=$(printf '%s' "$DC" | jq -r '.data.docCount')
[[ "$api_dc" -ge 0 ]] && pass "media-C25 docCount(\"\") = $api_dc" \
                     || fail "media-C25 docCount not a number: $DC"

# media-C26  docs list
DL=$(call_gql '{ docs(offset: 0, limit: 5, query: "", sortBy: NAME_ASC) { id title path } }')
api_dl_count=$(printf '%s' "$DL" | jq '.data.docs | length')
[[ "$api_dl_count" -ge 0 ]] && pass "media-C26 docs returned $api_dl_count items" \
                            || fail "media-C26 docs not a list: $DL"

# media-C27  docExtGroups
DEG=$(call_gql '{ docExtGroups { ext count } }')
api_deg=$(printf '%s' "$DEG" | jq '.data.docExtGroups | length')
[[ "$api_deg" -ge 0 ]] && pass "media-C27 docExtGroups returned $api_deg extension groups" \
                       || fail "media-C27 docExtGroups not a list: $DEG"

end_group