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

sed -i -e '/READ_SMS/d' ./app/src/main/AndroidManifest.xml
sed -i -e '/READ_CALL_LOG/d' ./app/src/main/AndroidManifest.xml
sed -i -e '/WRITE_CALL_LOG/d' ./app/src/main/AndroidManifest.xml
sed -i -e '/REQUEST_INSTALL_PACKAGES/d' ./app/src/main/AndroidManifest.xml
sed -i -e '/QUERY_ALL_PACKAGES/d' ./app/src/main/AndroidManifest.xml
sed -i -e '/REQUEST_DELETE_PACKAGES/d' ./app/src/main/AndroidManifest.xml

cat > ./keystore.properties <<EOF
storePassword=$ANDROID_STORE_PASSWORD
keyPassword=$ANDROID_KEY_PASSWORD
keyAlias=plain
storeFile=release.jks
EOF

cat > ./local.properties <<EOF
sdk.dir=/Users/$USER/Library/Android/sdk
EOF

cat > ./app/play-config.json <<EOF
$PLAY_STORE_CONFIG_JSON
EOF

./gradlew :app:bundleRelease || err_and_exit "build failed"

BUILD_FILE="PlainApp-$(getVersionName).aab"
mv ./app/build/outputs/bundle/googleRelease/app-google-release.aab $BUILD_FILE
