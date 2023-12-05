#!/bin/bash

function err_and_exit()
{
  echo "$1" >&2
  exit 1
}

function getVersionName() 
{
  echo $(grep versionName ./app/build.gradle.kts | awk -F '"' '{print $2}')
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

echo "Build PlainApp-$(getVersionName).apk..."

./gradlew assembleFreeRelease || err_and_exit "build failed"

echo "Copying apk file..."

BUILD_FILE="PlainApp-$(getVersionName).apk"

mv ./app/build/outputs/apk/free/release/app-free-release.apk ./app/build/outputs/apk/free/release/$BUILD_FILE