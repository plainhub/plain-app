package com.ismartcoding.plain.ui

/**
 * Returns `true` on Android, `false` on iOS / desktop / any other KMP target.
 *
 * Used to gate Android-only features (Bluetooth / DLNA / SMS / Call / Notification
 * listener / WebRTC / Nearby / Cast / Cast Player / Android Auto) in shared UI:
 * the feature entry is hidden or its route is not registered when this is `false`.
 *
 * iOS shares the entire Compose UI from `shared/commonMain`, so anything gated on
 * this returning `false` is invisible to iOS users by design.
 */
expect fun isAndroidOnly(): Boolean
