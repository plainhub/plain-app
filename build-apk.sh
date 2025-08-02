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

cat > ./keystore.properties <<EOF
storePassword=$ANDROID_STORE_PASSWORD
keyPassword=$ANDROID_KEY_PASSWORD
keyAlias=plain
storeFile=release.jks
EOF

cat > ./local.properties <<EOF
sdk.dir=/Users/$USER/Library/Android/sdk
EOF

# Build default APK (arm64-v8a)
./gradlew assembleGithubRelease || err_and_exit "assembleGithubRelease failed"
BUILD_FILE="PlainApp-$(getVersionName).apk"
mv ./app/build/outputs/apk/github/release/app-github-release.apk ./$BUILD_FILE

# Build armeabi-v7a APK
./gradlew clean
./gradlew assembleGithubRelease -PabiFilters=armeabi-v7a || err_and_exit "assembleGithubRelease armeabi-v7a failed"
ARMV7_BUILD_FILE="PlainApp-$(getVersionName)-armeabi-v7a.apk"
mv ./app/build/outputs/apk/github/release/app-github-release.apk ./$ARMV7_BUILD_FILE
