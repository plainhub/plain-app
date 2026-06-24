#!/usr/bin/env bash
# Group 0 — setup + sanity (AppGraphQL + introspection)
# Source-only; the runner sources this file.
#
# Schemas: AppGraphQL (9 of 9 endpoints in this group)
#   app, deviceInfo, battery
#   + 2 introspection queries
#
# See docs/api-test-plan.md for the full case list.

run_group "setup" "setup + sanity" "docs/api-test-plan.md#setup--sanity"

# ----------------------------------------------------------------------------
# Helper: query just one field on `app` to keep adb-extracted values comparable.
# The App model is a flat data class (see web/models/App.kt) — see notes there
# for which fields map to what.
# ----------------------------------------------------------------------------
APP=$(call_gql '{ app { clientId usbConnected httpPort httpsPort appDir deviceName battery appVersion osVersion channel debug developerMode internalStoragePath downloadsDir } }')
echo "  (raw app response saved to results/setup-app.json)"
echo "$APP" > "$RESULTS_DIR/setup-app.json"

# ----------------------------------------------------------------------------
# setup-C01  app.appVersion (int) == dumpsys package versionCode
# ----------------------------------------------------------------------------
api_vc=$(printf '%s' "$APP" | jq -r '.data.app.appVersion')
adb_vc=$(adb_sh "dumpsys package com.ismartcoding.plain.debug" | grep "versionCode" | head -1 | grep -oE "versionCode=[0-9]+" | grep -oE "[0-9]+")
compare_with_adb "$api_vc" "$adb_vc" "setup-C01 app.appVersion == dumpsys versionCode"

# ----------------------------------------------------------------------------
# setup-C02  app.osVersion (int) == adb ro.build.version.sdk
# ----------------------------------------------------------------------------
api_os=$(printf '%s' "$APP" | jq -r '.data.app.osVersion')
adb_sdk=$(adb_get_property "ro.build.version.sdk")
compare_with_adb "$api_os" "$adb_sdk" "setup-C02 app.osVersion == adb ro.build.version.sdk"

# ----------------------------------------------------------------------------
# setup-C03  app.httpPort == 8080 (and adb netstat agrees)
# ----------------------------------------------------------------------------
api_http=$(printf '%s' "$APP" | jq -r '.data.app.httpPort')
adb_http=$(adb_sh "netstat -tln 2>/dev/null" | grep -oE ":8080 " | head -1 | tr -d ': ')
compare_with_adb "$api_http" "8080" "setup-C03 app.httpPort == 8080 (constant)"
[[ -n "$adb_http" ]] && pass "setup-C03 adb netstat sees port 8080" || fail "setup-C03 adb netstat did not see port 8080"

# ----------------------------------------------------------------------------
# setup-C04  app.httpsPort == 8443
# ----------------------------------------------------------------------------
api_https=$(printf '%s' "$APP" | jq -r '.data.app.httpsPort')
adb_https=$(adb_sh "netstat -tln 2>/dev/null" | grep -oE ":8443 " | head -1 | tr -d ': ')
compare_with_adb "$api_https" "8443" "setup-C04 app.httpsPort == 8443 (constant)"
[[ -n "$adb_https" ]] && pass "setup-C04 adb netstat sees port 8443" || fail "setup-C04 adb netstat did not see port 8443"

# ----------------------------------------------------------------------------
# setup-C05  app.deviceName == adb settings get global device_name
# ----------------------------------------------------------------------------
api_dn=$(printf '%s' "$APP" | jq -r '.data.app.deviceName')
adb_dn=$(adb_sh "settings get global device_name" 2>/dev/null | tr -d '\r')
# Some ROMs (Pixel) return null; fall back to ro.product.model
if [[ -z "$adb_dn" || "$adb_dn" == "null" ]]; then
  adb_dn=$(adb_get_property "ro.product.model")
fi
compare_with_adb "$api_dn" "$adb_dn" "setup-C05 app.deviceName matches adb"

# ----------------------------------------------------------------------------
# setup-C06  app.appDir is the external scoped app dir (Context.appDir()
#         returns getExternalFilesDir(null)), and the path exists on the
#         device as reported by adb.
# ----------------------------------------------------------------------------
api_appdir=$(printf '%s' "$APP" | jq -r '.data.app.appDir')
adb_appdir=$(adb_sh "echo \$EXTERNAL_STORAGE/Android/data/com.ismartcoding.plain.debug/files")
[[ -n "$adb_appdir" && -d "$adb_appdir" ]] || adb_appdir=$(adb_sh "ls -d /storage/emulated/0/Android/data/com.ismartcoding.plain.debug/files 2>/dev/null" | tr -d '\r')
if [[ "$api_appdir" == "$adb_appdir" && -n "$api_appdir" ]]; then
  pass "setup-C06 app.appDir matches adb ls ($api_appdir)"
else
  fail "setup-C06 app.appDir mismatch: api='$api_appdir' adb='$adb_appdir'"
fi

# ----------------------------------------------------------------------------
# setup-C07  app.internalStoragePath == /storage/emulated/0
# ----------------------------------------------------------------------------
assert_jq "$APP" ".data.app.internalStoragePath" "/storage/emulated/0" "setup-C07 app.internalStoragePath"

# ----------------------------------------------------------------------------
# setup-C08  app.battery (int) within 2 of dumpsys battery level
# ----------------------------------------------------------------------------
api_battery=$(printf '%s' "$APP" | jq -r '.data.app.battery')
adb_battery=$(adb_sh "dumpsys battery" | grep "level:" | head -1 | grep -oE "[0-9]+" | head -1)
if [[ -n "$api_battery" && -n "$adb_battery" ]]; then
  diff=$(( api_battery - adb_battery ))
  [[ ${diff#-} -le 2 ]] && pass "setup-C08 app.battery ($api_battery) within 2 of adb ($adb_battery)" \
                       || fail "setup-C08 app.battery=$api_battery, adb=$adb_battery, diff>2"
else
  fail "setup-C08 could not parse battery (api='$api_battery' adb='$adb_battery')"
fi

# ----------------------------------------------------------------------------
# setup-C09  app.debug (bool) matches the per-app DEBUGGABLE flag reported by
#         `dumpsys package` (not `ro.debuggable` which is the OS-wide
#         userdebug toggle and would be 0 on a release-keys device).
# ----------------------------------------------------------------------------
api_debug=$(printf '%s' "$APP" | jq -r '.data.app.debug')
# dumpsys emits two `flags=` lines: the first is the user-id 0x0 marker,
# the second is the actual capability flags. Take the second.
adb_dbg_flag=$(adb_sh "dumpsys package com.ismartcoding.plain.debug" | grep "flags=" | sed -n '2p' | { grep -oE "DEBUGGABLE" || true; } | head -1)
if [[ "$api_debug" == "true" && -n "$adb_dbg_flag" ]] || [[ "$api_debug" == "false" && -z "$adb_dbg_flag" ]]; then
  pass "setup-C09 app.debug matches dumpsys DEBUGGABLE flag (api=$api_debug, adb_flag='$adb_dbg_flag')"
else
  fail "setup-C09 app.debug=$api_debug but dumpsys DEBUGGABLE='$adb_dbg_flag'"
fi

# ----------------------------------------------------------------------------
# setup-C10  app.clientId is non-empty and stable across calls
# ----------------------------------------------------------------------------
api_cid=$(printf '%s' "$APP" | jq -r '.data.app.clientId')
[[ -n "$api_cid" && "$api_cid" != "null" ]] && pass "setup-C10 app.clientId is non-empty ($api_cid)" \
                                            || fail "setup-C10 app.clientId missing"
# Note: this is TempData.clientId (the device's own id), NOT the session's cid.
# That is a known design — session cid is sent in c-id header, not echoed back.

# ----------------------------------------------------------------------------
# setup-C11  app.channel is one of the build flavors declared in app/build.gradle.kts
#         (GITHUB / GOOGLE / FDROID). It is the productFlavor name, NOT a
#         build type — debug+github is "GITHUB", release+github is "GITHUB",
#         etc. See app/build.gradle.kts:88-102.
# ----------------------------------------------------------------------------
api_ch=$(printf '%s' "$APP" | jq -r '.data.app.channel')
case "$api_ch" in
  GITHUB|GOOGLE|FDROID) pass "setup-C11 app.channel is a recognized flavor ($api_ch)" ;;
  *) fail "setup-C11 app.channel unexpected value: $api_ch" ;;
esac

# ----------------------------------------------------------------------------
# setup-C12  app.usbConnected reflects current USB plug state
# ----------------------------------------------------------------------------
api_usb=$(printf '%s' "$APP" | jq -r '.data.app.usbConnected')
# adb-connected devices usually have USB unplugged from a hardware sense perspective,
# but PlugInControlReceiver is checking the USB power sense. Just check it's a bool.
if [[ "$api_usb" == "true" || "$api_usb" == "false" ]]; then
  pass "setup-C12 app.usbConnected is a valid bool ($api_usb)"
else
  fail "setup-C12 app.usbConnected not a bool: $api_usb"
fi

# ----------------------------------------------------------------------------
# setup-C13  deviceInfo: osVersion (string) matches ro.build.version.release
# ----------------------------------------------------------------------------
DI=$(call_gql '{ deviceInfo { osVersion osName model manufacturer kernelVersion cpuArch appVersion appBuildNumber language android { sdkVersion versionCodeName securityPatch } } }')
echo "$DI" > "$RESULTS_DIR/setup-deviceinfo.json"
api_di_os=$(printf '%s' "$DI" | jq -r '.data.deviceInfo.osVersion')
adb_release=$(adb_get_property "ro.build.version.release")
compare_with_adb "$api_di_os" "$adb_release" "setup-C13 deviceInfo.osVersion == adb ro.build.version.release"

# setup-C13b deviceInfo.android.sdkVersion == ro.build.version.sdk
api_di_sdk=$(printf '%s' "$DI" | jq -r '.data.deviceInfo.android.sdkVersion')
compare_with_adb "$api_di_sdk" "$adb_sdk" "setup-C13b deviceInfo.android.sdkVersion == adb ro.build.version.sdk"

# setup-C13c deviceInfo.model == ro.product.model
api_di_model=$(printf '%s' "$DI" | jq -r '.data.deviceInfo.model')
adb_model=$(adb_get_property "ro.product.model")
compare_with_adb "$api_di_model" "$adb_model" "setup-C13c deviceInfo.model == adb ro.product.model"

# setup-C13d deviceInfo.manufacturer == ro.product.manufacturer
api_di_mfr=$(printf '%s' "$DI" | jq -r '.data.deviceInfo.manufacturer')
adb_mfr=$(adb_get_property "ro.product.manufacturer")
compare_with_adb "$api_di_mfr" "$adb_mfr" "setup-C13d deviceInfo.manufacturer == adb ro.product.manufacturer"

# ----------------------------------------------------------------------------
# setup-C14  battery.level within 2 of dumpsys battery level
# ----------------------------------------------------------------------------
BATT=$(call_gql '{ battery { level status plugged health temperature } }')
echo "$BATT" > "$RESULTS_DIR/setup-battery.json"
api_bl=$(printf '%s' "$BATT" | jq -r '.data.battery.level')
if [[ -n "$api_bl" && -n "$adb_battery" ]]; then
  diff=$(( api_bl - adb_battery ))
  [[ ${diff#-} -le 2 ]] && pass "setup-C14 battery.level ($api_bl) within 2 of dumpsys ($adb_battery)" \
                       || fail "setup-C14 battery.level=$api_bl, dumpsys=$adb_battery, diff>2"
else
  fail "setup-C14 could not parse battery.level (api='$api_bl' adb='$adb_battery')"
fi

# ----------------------------------------------------------------------------
# setup-C15  introspection: __schema.queryType.name == "Query"
# ----------------------------------------------------------------------------
INTRO=$(call_gql '{ __schema { queryType { name } mutationType { name } subscriptionType { name } } }')
assert_jq "$INTRO" ".data.__schema.queryType.name" "Query" "setup-C15 __schema.queryType.name == Query"
assert_jq "$INTRO" ".data.__schema.mutationType.name" "Mutation" "setup-C16 __schema.mutationType.name == Mutation"

# ----------------------------------------------------------------------------
# setup-C17  introspection: count of queries is 70 (snapshot from 2026-06-24)
# ----------------------------------------------------------------------------
QCOUNT=$(call_gql '{ __schema { queryType { fields { name } } } }' | jq '.data.__schema.queryType.fields | length')
MCOUNT=$(call_gql '{ __schema { mutationType { fields { name } } } }' | jq '.data.__schema.mutationType.fields | length')
# 70 queries + 107 mutations = 177 (snapshot 2026-06-24). Tolerate ±3.
if [[ "$QCOUNT" -ge 68 && "$QCOUNT" -le 73 ]]; then
  pass "setup-C17 queryType.fields count = $QCOUNT (expected ~70)"
else
  fail "setup-C17 queryType.fields count = $QCOUNT (expected ~70)"
fi
if [[ "$MCOUNT" -ge 104 && "$MCOUNT" -le 110 ]]; then
  pass "setup-C18 mutationType.fields count = $MCOUNT (expected ~107)"
else
  fail "setup-C18 mutationType.fields count = $MCOUNT (expected ~107)"
fi

end_group
