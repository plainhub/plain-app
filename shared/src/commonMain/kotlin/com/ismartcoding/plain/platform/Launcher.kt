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