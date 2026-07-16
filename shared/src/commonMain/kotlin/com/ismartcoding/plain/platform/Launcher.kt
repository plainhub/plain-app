package com.ismartcoding.plain.platform

/**
 * Open a URL in the system browser / default app.
 * Android: Intent(ACTION_VIEW, uri)
 * iOS: UIApplication.openURL
 */
expect fun launchUrl(url: String)

/**
 * Open the platform app settings screen so the user can manage permissions.
 */
expect fun openAppSettings()

/**
 * Share plain text via the system share sheet.
 */
expect fun shareText(text: String)

/**
 * Share a single file via the system share sheet (uses FileProvider on Android).
 */
expect fun shareFile(path: String)

/**
 * Share a single file via the system share sheet, overriding the displayed file
 * name with [displayName] (copies the file to a temp cache file with that name on
 * Android before sharing via FileProvider).
 */
expect fun shareFileAs(path: String, displayName: String)

/**
 * Share multiple files via the system share sheet.
 */
expect fun shareFiles(paths: List<String>)

/**
 * Open a file with the default external app (ACTION_VIEW on Android).
 */
expect fun openFileExternal(path: String)

/**
 * Whether the file at [path] can be safely shared via the system share sheet.
 * On Android, this checks existence, readability, and skips system/apex paths
 * that cannot be shared. On iOS, returns false (no equivalent file-sharing
 * semantics for protected paths).
 */
expect fun isFileShareable(path: String): Boolean

/**
 * Restart the app process.
 */
expect fun relaunchApp()