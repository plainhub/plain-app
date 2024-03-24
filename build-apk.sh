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

./gradlew assembleGithubRelease || err_and_exit "build failed"
./gradlew assembleChinaRelease || err_and_exit "build failed"

BUILD_FILE="PlainApp-$(getVersionName).apk"
mv ./app/build/outputs/apk/github/release/app-github-release.apk ./app/build/outputs/apk/github/release/$BUILD_FILE

BUILD_FILE="PlainApp-$(getVersionName)-china.apk"
mv ./app/build/outputs/apk/china/release/app-china-release.apk ./app/build/outputs/apk/china/release/$BUILD_FILE
