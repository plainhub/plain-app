package com.ismartcoding.plain.platform

/**
 * Share a crash report via the system share sheet (email-style).
 *
 * On Android this constructs an `ACTION_SEND` intent with the crash report
 * attached as a temp file via FileProvider, plus optional app logs. On iOS this
 * is a no-op (would require a `UIActivityViewController` with the report as an
 * attachment).
 *
 * @param report          Crash report body text (already formatted).
 * @param includeAppLogs  When true, also attach the most recent app log lines.
 */
expect fun shareCrashReport(report: String, includeAppLogs: Boolean)
