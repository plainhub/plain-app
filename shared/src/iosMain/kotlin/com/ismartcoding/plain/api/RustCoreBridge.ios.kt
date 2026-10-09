package com.ismartcoding.plain.api

import com.ismartcoding.plain.rustprefs.plain_http_start
import com.ismartcoding.plain.rustprefs.plain_http_stop
import com.ismartcoding.plain.rustprefs.plain_core_start
import com.ismartcoding.plain.rustprefs.plain_prefs_string_free
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
internal actual object RustCoreBridge {
    actual fun startPublic(configJson: String): String = memScoped {
        val result = checkNotNull(plain_http_start(configJson.cstr.ptr))
        try { result.toKString() }
        finally { plain_prefs_string_free(result) }
    }
    actual fun stopPublic() {
        val result = plain_http_stop() ?: return
        try { error(result.toKString()) } finally { plain_prefs_string_free(result) }
    }

    actual fun start(databasePath: String, token: String): Int = memScoped {
        val result = plain_core_start(databasePath.cstr.ptr, token.cstr.ptr) ?: error("Rust core returned no port")
        try {
            val value = result.toKString()
            check(!value.startsWith("ERROR:")) { value.removePrefix("ERROR:") }
            value.toInt()
        } finally { plain_prefs_string_free(result) }
    }
}
