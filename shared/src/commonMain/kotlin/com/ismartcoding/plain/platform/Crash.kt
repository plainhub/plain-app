package com.ismartcoding.plain.platform

/**
 * Write the crash report (with device info and app logs) to a cache file and
 * return its absolute path so the caller can navigate to a text viewer.
 *
 * @param report  Crash report body text (already formatted).
 * @return Absolute path of the written file, or empty string on failure.
 */
expect fun writeCrashReport(report: String): String
