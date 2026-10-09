package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * The Vue bundle lives in the APK assets (Android) or the app bundle (iOS),
 * which the Rust HTTP listener cannot read. The host unpacks it once into a
 * version-stamped directory under the app data dir and passes the path with
 * the public server config; Rust then owns routing, cache headers, the SPA
 * fallback and the `__SERVER_TIME__` bootstrap.
 *
 * Embedding the bundle into the Rust crate instead would duplicate ~11 MB in
 * every `.so`/framework and force a full plain-rs rebuild for web-only changes.
 */
object RustWebAssets {
    /**
     * Unpacks the bundle when the stamped directory for the running app version
     * is missing or incomplete. Returns an empty string when the bundle is
     * unavailable, which makes Rust skip every web route instead of serving a
     * broken shell. Must run before the public server starts so the root
     * reaches the Rust server.
     */
    fun ensure(): String = runCatching { extractWebAssets() }.getOrElse { error ->
        LogCat.e("Failed to extract web assets", error)
        ""
    }
}

internal expect fun extractWebAssets(): String
