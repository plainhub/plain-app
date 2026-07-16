package com.ismartcoding.plain.platform

/**
 * Download the latest app update from the URL stored in `UpdateInfoPreference`
 * to the platform's cache directory. Reports progress via `UpdateDownloadProgressEvent`,
 * completion via `UpdateDownloadCompleteEvent`, and failures via
 * `UpdateDownloadFailedEvent`. Updates `UpdateInfoPreference.downloadedApkPath`
 * on completion.
 *
 * No-op (emits `UpdateDownloadFailedEvent`) on platforms without an installer
 * (iOS). The Android implementation downloads the APK to `cacheDir/plain-update.apk`
 * using the OkHttp download client.
 */
expect fun downloadUpdateAsync()

/**
 * Cancel any in-progress update download and clear the stored downloaded path.
 * No-op on platforms without an update downloader (iOS).
 */
expect fun cancelUpdateDownloadAsync()
