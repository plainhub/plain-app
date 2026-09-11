#!/bin/bash

function err_and_exit()
{
  echo "$1" >&2
  try_print_missing_rules
  exit 1
}

function getVersionName()
{
  echo $(grep versionName ./app/build.gradle.kts | awk -F '"' '{print $2}')
}

function try_print_missing_rules()
{
  MISSING_FILE="./app/build/outputs/mapping/githubRelease/missing_rules.txt"
  if [ -f "$MISSING_FILE" ]; then
    echo "========== R8 missing_rules.txt =========="
    cat "$MISSING_FILE"
    echo "=========================================="
  else
    echo "[WARN] missing_rules.txt not found at $MISSING_FILE"
  fi
}

# Guards against stale packaging now that the second ABI build no longer
# runs after a clean: each APK must contain exactly its own ABI.
function assert_apk_abi()
{
  local apk="$1" want="$2" forbid="$3"
  if ! unzip -l "$apk" | grep -q "lib/$want/"; then
    err_and_exit "$apk: missing lib/$want (stale packaging?)"
  fi
  if unzip -l "$apk" | grep -q "lib/$forbid/"; then
    err_and_exit "$apk: unexpectedly contains lib/$forbid (stale packaging?)"
  fi
}

cat > ./keystore.properties <<EOF
storePassword=$ANDROID_STORE_PASSWORD
keyPassword=$ANDROID_KEY_PASSWORD
keyAlias=release
storeFile=release.jks
EOF

if [ -n "$ANDROID_HOME" ]; then
  SDK_DIR="$ANDROID_HOME"
elif [ -d "$HOME/Library/Android/sdk" ]; then
  SDK_DIR="$HOME/Library/Android/sdk"
else
  SDK_DIR="/usr/local/lib/android/sdk"
fi
cat > ./local.properties <<EOF
sdk.dir=$SDK_DIR
EOF

# Build arm64-v8a APK (64-bit, for modern devices)
./gradlew assembleGithubRelease || err_and_exit "assembleGithubRelease failed"
BUILD_FILE="PlainApp-$(getVersionName)-64bit-Recommended.apk"
mv ./app/build/outputs/apk/github/release/app-github-release.apk ./$BUILD_FILE

# Build armeabi-v7a APK (no clean: Gradle input tracking re-runs only what
# the ABI switch affects, shared-module compile outputs stay up to date)
./gradlew assembleGithubRelease -PabiFilters=armeabi-v7a || err_and_exit "assembleGithubRelease armeabi-v7a failed"
ARMV7_BUILD_FILE="PlainApp-$(getVersionName)-Old-32bit.apk"
mv ./app/build/outputs/apk/github/release/app-github-release.apk ./$ARMV7_BUILD_FILE

assert_apk_abi "$BUILD_FILE" arm64-v8a armeabi-v7a
assert_apk_abi "$ARMV7_BUILD_FILE" armeabi-v7a arm64-v8a
