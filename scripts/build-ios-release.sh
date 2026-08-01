#!/bin/bash
set -euo pipefail

# iOS Build & Release Script
# Usage:
#   ./scripts/build-ios-release.sh bump     - Increment build number
#   ./scripts/build-ios-release.sh debug    - Build debug (simulator)
#   ./scripts/build-ios-release.sh archive  - Build archive for release
#   ./scripts/build-ios-release.sh export   - Export IPA from archive
#   ./scripts/build-ios-release.sh release  - Full: bump + archive + export

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
XCODE_PROJECT="$PROJECT_ROOT/iosApp/iosApp.xcodeproj"
SCHEME="iosApp"
ARCHIVE_PATH="$PROJECT_ROOT/build/ios/PlainApp.xcarchive"
EXPORT_DIR="$PROJECT_ROOT/build/ios/export"
EXPORT_PLIST="$PROJECT_ROOT/build/ios/ExportOptions.plist"

command=${1:-debug}

bump_build_number() {
    local pbxproj="$XCODE_PROJECT/project.pbxproj"
    local current=$(grep -m1 'CURRENT_PROJECT_VERSION' "$pbxproj" | awk -F '= ' '{print $2}' | tr -d ';')
    local next=$((current + 1))
    sed -i '' "s/CURRENT_PROJECT_VERSION = $current;/CURRENT_PROJECT_VERSION = $next;/g" "$pbxproj"
    echo "Build number bumped: $current -> $next"
}

build_debug() {
    echo "=== Building Kotlin Framework (Debug) ==="
    cd "$PROJECT_ROOT"
    ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

    echo "=== Building iOS App (Debug, Simulator) ==="
    xcodebuild \
        -project "$XCODE_PROJECT" \
        -scheme "$SCHEME" \
        -configuration Debug \
        -sdk iphonesimulator \
        -destination 'generic/platform=iOS Simulator' \
        build
    echo "=== Debug build succeeded ==="
}

build_archive() {
    echo "=== Building Kotlin Framework (Release) ==="
    cd "$PROJECT_ROOT"
    ./gradlew :shared:linkReleaseFrameworkIosSimulatorArm64

    echo "=== Resolving Swift Package Dependencies ==="
    xcodebuild \
        -project "$XCODE_PROJECT" \
        -scheme "$SCHEME" \
        -resolvePackageDependencies

    echo "=== Archiving iOS App ==="
    mkdir -p "$(dirname "$ARCHIVE_PATH")"
    xcodebuild \
        -project "$XCODE_PROJECT" \
        -scheme "$SCHEME" \
        -configuration Release \
        -archivePath "$ARCHIVE_PATH" \
        -destination 'generic/platform=iOS' \
        archive
    echo "=== Archive succeeded: $ARCHIVE_PATH ==="
}

export_ipa() {
    if [ ! -d "$ARCHIVE_PATH" ]; then
        echo "ERROR: Archive not found at $ARCHIVE_PATH"
        echo "Run './scripts/build-ios-release.sh archive' first."
        exit 1
    fi

    echo "=== Exporting IPA ==="
    mkdir -p "$EXPORT_DIR"

    cat > "$EXPORT_PLIST" << 'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>method</key>
    <string>app-store</string>
    <key>teamID</key>
    <string>$(IOS_TEAM_ID)</string>
    <key>uploadBitcode</key>
    <false/>
    <key>uploadSymbols</key>
    <true/>
    <key>stripSwiftSymbols</key>
    <true/>
    <key>compileBitcode</key>
    <false/>
</dict>
</plist>
PLIST

    if [ -z "${IOS_TEAM_ID:-}" ]; then
        echo "ERROR: IOS_TEAM_ID environment variable not set."
        echo "Find your Team ID at https://developer.apple.com/account/ (Membership Details)."
        echo "Set it with: export IOS_TEAM_ID=YOUR_TEAM_ID"
        exit 1
    fi

    sed -i '' "s/\$(IOS_TEAM_ID)/$IOS_TEAM_ID/g" "$EXPORT_PLIST"

    xcodebuild \
        -exportArchive \
        -archivePath "$ARCHIVE_PATH" \
        -exportOptionsPlist "$EXPORT_PLIST" \
        -exportPath "$EXPORT_DIR"

    echo "=== IPA exported to: $EXPORT_DIR ==="
    ls -la "$EXPORT_DIR"
}

upload_testflight() {
    local ipa=$(find "$EXPORT_DIR" -name "*.ipa" -type f | head -1)
    if [ -z "$ipa" ]; then
        echo "ERROR: No IPA found in $EXPORT_DIR"
        exit 1
    fi

    if [ -z "${APP_STORE_CONNECT_KEY_ID:-}" ] || [ -z "${APP_STORE_CONNECT_ISSUER_ID:-}" ]; then
        echo "ERROR: APP_STORE_CONNECT_KEY_ID and APP_STORE_CONNECT_ISSUER_ID must be set."
        echo "Also place your API key at ~/.appstoreconnect/private_keys/AuthKey_<KEY_ID>.p8"
        exit 1
    fi

    echo "=== Uploading to TestFlight ==="
    xcrun altool \
        --upload-app \
        --type ios \
        --file "$ipa" \
        --apiKey "$APP_STORE_CONNECT_KEY_ID" \
        --apiIssuer "$APP_STORE_CONNECT_ISSUER_ID" \
        --verbose

    echo "=== Upload complete. Check App Store Connect → TestFlight ==="
}

case "$command" in
    bump)
        bump_build_number
        ;;
    debug)
        build_debug
        ;;
    archive)
        build_archive
        ;;
    export)
        export_ipa
        ;;
    upload)
        upload_testflight
        ;;
    release)
        bump_build_number
        build_archive
        export_ipa
        echo "=== Release build ready ==="
        echo "To upload: ./scripts/build-ios-release.sh upload"
        ;;
    *)
        echo "Usage: $0 {bump|debug|archive|export|upload|release}"
        exit 1
        ;;
esac
