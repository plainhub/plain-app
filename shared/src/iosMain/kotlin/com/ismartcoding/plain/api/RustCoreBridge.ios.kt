package com.ismartcoding.plain.api

import com.ismartcoding.plain.rustprefs.plain_http_stop
import com.ismartcoding.plain.rustprefs.plain_core_start
import com.ismartcoding.plain.rustprefs.plain_prefs_string_free
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
internal actual object RustCoreBridge {
    actual fun start(databasePath: String, token: String, configJson: String): String = memScoped {
        val result = plain_core_start(databasePath.cstr.ptr, token.cstr.ptr, configJson.cstr.ptr) ?: error("Rust core returned no response")
        try { result.toKString() } finally { plain_prefs_string_free(result) }
    }
    actual fun stop() {
        val result = plain_http_stop() ?: return
        try { error(result.toKString()) } finally { plain_prefs_string_free(result) }
    }
}
