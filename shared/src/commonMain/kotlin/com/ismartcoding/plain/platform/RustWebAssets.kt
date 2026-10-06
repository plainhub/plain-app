package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.SystemPrefs

/**
 * The Vue bundle lives in the APK assets (Android) or the app bundle (iOS),
 * which the Rust HTTP listener cannot read. The host unpacks it once into a
 * version-stamped directory under the app data dir and publishes the path in
 * the `web_asset_root` pref; Rust then owns routing, cache headers, the SPA
 * fallback and the `__SERVER_TIME__` bootstrap.
 *
 * Embedding the bundle into the Rust crate instead would duplicate ~11 MB in
 * every `.so`/framework and force a full plain-rs rebuild for web-only changes.
 */
object RustWebAssets {
    /**
     * Unpacks the bundle when the stored root no longer matches the running
     * app version, then publishes it for Rust. Must run after
     * `RustContentApi.start()` so the pref write reaches the Rust store.
     */
    fun ensure(): String {
        val root = runCatching { extractWebAssets() }.getOrElse { error ->
            LogCat.e("Failed to extract web assets", error)
            return ""
        }
        SystemPrefs.webAssetRoot.value = root
        SystemPrefs.webAssetVersion.value = webAssetVersion()
        return root
    }

    /** Re-published to Rust on every start so a bundle change is picked up. */
    private fun webAssetVersion(): String = try {
        com.ismartcoding.plain.platform.appVersionName()
    } catch (_: Exception) {
        ""
    }
}

internal expect fun extractWebAssets(): String

internal expect fun appVersionName(): String
