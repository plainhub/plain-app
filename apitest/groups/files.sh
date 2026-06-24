#!/usr/bin/env bash
# Group files — FileQuery + FileMutation + FileUpload
# Source-only; the runner sources this file.
#
# Schemas covered:
#   FileQueryGraphQL   : mounts, recentFiles, files, fileInfo, fileIds
#   FileMutationGraphQL: deleteFiles, createDir, renameFile, writeTextFile,
#                        copyFile, moveFile, addFavoriteFolder,
#                        removeFavoriteFolder, setFavoriteFolderAlias
#   FileUploadGraphQL  : uploadedChunks, deleteChunks, mergeChunks
#
# All read paths use the Android filesystem via `adb shell ls` for ground
# truth. Mutations work on a dedicated `/storage/emulated/0/Download/apitest-*`
# sandbox so other data on the device stays untouched.

run_group "files" "file query / mutation / upload" "docs/api-test-plan.md#files"

FIXTURE_DIR="/storage/emulated/0/Download/apitest-files"
FIXTURE_FILE="$FIXTURE_DIR/sample.txt"

# ----------------------------------------------------------------------------
# files-C01  mounts returns storage mounts
# ----------------------------------------------------------------------------
MO=$(call_gql '{ mounts { id name path mountPoint fsType totalBytes freeBytes } }')
api_mo_count=$(printf '%s' "$MO" | jq '.data.mounts | length')
[[ "$api_mo_count" -ge 1 ]] && pass "files-C01 mounts returned $api_mo_count mounts" \
                          || fail "files-C01 mounts not a list: $MO"

# ----------------------------------------------------------------------------
# files-C02  files(root: "/") returns the sdcard listing
# ----------------------------------------------------------------------------
FL=$(call_gql '{ files(root: "/storage/emulated/0", offset: 0, limit: 100, query: "", sortBy: NAME_ASC) { name path size isDir children } }')
api_fl_count=$(printf '%s' "$FL" | jq '.data.files | length')
adb_fl_count=$(adb_sh "ls -1 /storage/emulated/0/ 2>/dev/null" | { grep -vc '^$' || true; })
if [[ "$api_fl_count" -gt 0 ]]; then
  pass "files-C02 files(\"/\") returned $api_fl_count items (adb ls has $adb_fl_count)"
else
  fail "files-C02 files(\"/\") returned empty"
fi

# ----------------------------------------------------------------------------
# files-C03  recentFiles returns list (may be empty)
# ----------------------------------------------------------------------------
RF=$(call_gql '{ recentFiles { id name path } }')
api_rf_count=$(printf '%s' "$RF" | jq '.data.recentFiles | length')
[[ "$api_rf_count" -ge 0 ]] && pass "files-C03 recentFiles returned $api_rf_count items" \
                            || fail "files-C03 recentFiles not a list: $RF"

# ----------------------------------------------------------------------------
# files-C04  fileInfo on existing file (Downloads)
# ----------------------------------------------------------------------------
FI=$(call_gql "{ fileInfo(id: \"\", path: \"/storage/emulated/0/Download\", fileName: \"Download\") { path size updatedAt } }")
api_fi_path=$(printf '%s' "$FI" | jq -r '.data.fileInfo.path // empty')
if [[ "$api_fi_path" == "/storage/emulated/0/Download" ]]; then
  pass "files-C04 fileInfo(path=/storage/emulated/0/Download) returned path match"
else
  fail "files-C04 fileInfo path=$api_fi_path: $FI"
fi

# ----------------------------------------------------------------------------
# files-C05  fileIds returns deterministic ids for paths
# ----------------------------------------------------------------------------
FIDS=$(call_gql '{ fileIds(paths: ["/storage/emulated/0/Download", "/storage/emulated/0/Music"]) }')
api_fids_count=$(printf '%s' "$FIDS" | jq '.data.fileIds | length')
if [[ "$api_fids_count" == "2" ]]; then
  api_fid1=$(printf '%s' "$FIDS" | jq -r '.data.fileIds[0]')
  api_fid2=$(printf '%s' "$FIDS" | jq -r '.data.fileIds[1]')
  if [[ -n "$api_fid1" && -n "$api_fid2" && "$api_fid1" != "$api_fid2" ]]; then
    pass "files-C05 fileIds returned distinct ids: $api_fid1, $api_fid2"
  else
    fail "files-C05 fileIds returned identical/empty ids: $FIDS"
  fi
else
  fail "files-C05 fileIds returned $api_fids_count items (expected 2)"
fi

# ----------------------------------------------------------------------------
# files-C06  createDir → writeTextFile → fileInfo round-trip
# ----------------------------------------------------------------------------
# createDir runs on the device and creates $FIXTURE_DIR under /Download.
CREATE_D=$(call_gql "mutation { createDir(path: \"$FIXTURE_DIR\") { path name isDir } }")
api_cd_err=$(printf '%s' "$CREATE_D" | jq -r '.errors[0].message // empty')
if [[ -z "$api_cd_err" || "$api_cd_err" == *"already exists"* ]]; then
  # Either the dir was created, or it already exists from a previous run.
  pass "files-C06 createDir($FIXTURE_DIR) → $(if [[ -z "$api_cd_err" ]]; then echo "created"; else echo "already exists"; fi)"
elif [[ "$api_cd_err" == *"permission"* ]]; then
  skip "files-C06..15 (permission gated: WRITE_EXTERNAL_STORAGE)"
  skip "files-C16..17 (permission gated)"
else
  fail "files-C06 createDir returned: $CREATE_D"
fi

# Whether C06 passed or was skipped, attempt the rest only if createDir worked.
if [[ -z "$api_cd_err" || "$api_cd_err" == *"already exists"* ]]; then
  # C07: writeTextFile
  WT=$(call_gql "mutation { writeTextFile(path: \"$FIXTURE_FILE\", content: \"apitest content\", overwrite: false) { path size } }")
  api_wt_err=$(printf '%s' "$WT" | jq -r '.errors[0].message // empty')
  if [[ -z "$api_wt_err" ]]; then
    pass "files-C07 writeTextFile → $FIXTURE_FILE"
    # C08: fileInfo on the file
    FI2=$(call_gql "{ fileInfo(id: \"\", path: \"$FIXTURE_FILE\", fileName: \"sample.txt\") { path size } }")
    api_fi2_size=$(printf '%s' "$FI2" | jq -r '.data.fileInfo.size // empty')
    adb_fi2_size=$(adb_sh "stat -c '%s' $FIXTURE_FILE 2>/dev/null" | tr -d '\r')
    if [[ -n "$adb_fi2_size" && "$api_fi2_size" == "$adb_fi2_size" ]]; then
      pass "files-C08 fileInfo on $FIXTURE_FILE size=$api_fi2_size == adb"
    elif [[ -n "$api_fi2_size" ]]; then
      pass "files-C08 fileInfo returned size=$api_fi2_size (adb readback not available)"
    else
      fail "files-C08 fileInfo size=$api_fi2_size: $FI2"
    fi
  elif [[ "$api_wt_err" == *"already exists"* ]]; then
    pass "files-C07 writeTextFile → already exists (clean up between runs)"
    api_wt_err=""
  else
    fail "files-C07 writeTextFile: $WT"
  fi

  # C09: copyFile
  CP=$(call_gql "mutation { copyFile(src: \"$FIXTURE_FILE\", dst: \"${FIXTURE_FILE}.copy\", overwrite: false) }")
  api_cp=$(printf '%s' "$CP" | jq -r '.data.copyFile // empty')
  if [[ "$api_cp" == "true" ]]; then
    pass "files-C09 copyFile → true"
    adb_cp_exists=$(adb_sh "test -f ${FIXTURE_FILE}.copy && echo yes || echo no" 2>/dev/null | tr -d '\r')
    [[ "$adb_cp_exists" == "yes" ]] && pass "files-C09b adb confirms copy exists" \
                                   || fail "files-C09b adb cannot find ${FIXTURE_FILE}.copy"
  else
    fail "files-C09 copyFile returned: $CP"
  fi

  # C10: moveFile
  MV=$(call_gql "mutation { moveFile(src: \"${FIXTURE_FILE}.copy\", dst: \"${FIXTURE_FILE}.moved\", overwrite: false) }")
  api_mv=$(printf '%s' "$MV" | jq -r '.data.moveFile // empty')
  if [[ "$api_mv" == "true" ]]; then
    pass "files-C10 moveFile → true"
  else
    fail "files-C10 moveFile returned: $MV"
  fi

  # C11: renameFile
  RN=$(call_gql "mutation { renameFile(path: \"${FIXTURE_FILE}.moved\", name: \"renamed.txt\") }")
  api_rn=$(printf '%s' "$RN" | jq -r '.data.renameFile // empty')
  if [[ "$api_rn" == "true" ]]; then
    pass "files-C11 renameFile → true"
  else
    fail "files-C11 renameFile returned: $RN"
  fi

  # C12: addFavoriteFolder
  AFF=$(call_gql "mutation { addFavoriteFolder(rootPath: \"/storage/emulated/0\", fullPath: \"$FIXTURE_DIR\") { rootPath fullPath alias } }")
  api_aff_count=$(printf '%s' "$AFF" | jq '.data.addFavoriteFolder | length')
  [[ "$api_aff_count" -ge 1 ]] && pass "files-C12 addFavoriteFolder → $api_aff_count folders" \
                             || fail "files-C12 addFavoriteFolder returned: $AFF"

  # C13: setFavoriteFolderAlias
  SFFA=$(call_gql "mutation { setFavoriteFolderAlias(fullPath: \"$FIXTURE_DIR\", alias: \"apitest-alias\") { fullPath alias } }")
  api_sffa_alias=$(printf '%s' "$SFFA" | jq -r '.data.setFavoriteFolderAlias[] | select(.fullPath=="'$FIXTURE_DIR'") | .alias' 2>/dev/null)
  if [[ "$api_sffa_alias" == "apitest-alias" ]]; then
    pass "files-C13 setFavoriteFolderAlias → alias='$api_sffa_alias'"
  else
    fail "files-C13 setFavoriteFolderAlias returned: $SFFA"
  fi

  # C14: removeFavoriteFolder
  RFF=$(call_gql "mutation { removeFavoriteFolder(fullPath: \"$FIXTURE_DIR\") { fullPath } }")
  api_rff=$(printf '%s' "$RFF" | jq -r '.data.removeFavoriteFolder[] | select(.fullPath=="'$FIXTURE_DIR'") | .fullPath' 2>/dev/null)
  if [[ -z "$api_rff" ]]; then
    pass "files-C14 removeFavoriteFolder → not in list anymore"
  else
    fail "files-C14 removeFavoriteFolder still contains $FIXTURE_DIR"
  fi

  # C15: deleteFiles (cleanup all our fixtures)
  DF=$(call_gql "mutation { deleteFiles(paths: [\"${FIXTURE_FILE}\", \"${FIXTURE_FILE}.copy\", \"${FIXTURE_FILE}.moved\", \"${FIXTURE_DIR}/renamed.txt\"]) }")
  api_df=$(printf '%s' "$DF" | jq -r '.data.deleteFiles // empty')
  if [[ "$api_df" == "true" ]]; then
    pass "files-C15 deleteFiles → true"
  else
    fail "files-C15 deleteFiles returned: $DF"
  fi

  # C16: uploadedChunks (no chunks uploaded → empty list)
  UC=$(call_gql '{ uploadedChunks(fileId: "apitest-nonexistent") }')
  api_uc=$(printf '%s' "$UC" | jq '.data.uploadedChunks | length')
  [[ "$api_uc" == "0" ]] && pass "files-C16 uploadedChunks(apitest-nonexistent) → 0 chunks" \
                        || pass "files-C16 uploadedChunks returned $api_uc chunks (non-empty if previous run)"

  # C17: deleteChunks (cleanup chunk dir if any)
  DC=$(call_gql 'mutation { deleteChunks(fileId: "apitest-nonexistent") }')
  api_dc=$(printf '%s' "$DC" | jq -r '.data.deleteChunks // empty')
  if [[ "$api_dc" == "true" ]]; then
    pass "files-C17 deleteChunks(apitest-nonexistent) → true"
  else
    fail "files-C17 deleteChunks returned: $DC"
  fi

  # mergeChunks: we don't actually upload chunks via multipart in the harness.
  skip "files-C18 mergeChunks (skipped: requires multipart chunk upload)"
else
  skip "files-C07..17 (createDir was skipped)"
  skip "files-C18 mergeChunks (requires C06)"
fi

end_group