package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.rustprefs.plain_prefs_open
import com.ismartcoding.plain.rustprefs.plain_prefs_remove
import com.ismartcoding.plain.rustprefs.plain_prefs_set
import com.ismartcoding.plain.rustprefs.plain_prefs_snapshot
import com.ismartcoding.plain.rustprefs.plain_prefs_string_free
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
internal actual object RustPrefsBridge {
    actual fun open(path: String) = memScoped {
        checkError(plain_prefs_open(path.cstr.ptr))
    }

    actual fun snapshot(): String {
        val result = plain_prefs_snapshot() ?: error("Rust preferences returned no snapshot")
        try {
            return result.toKString().also {
                if (it.startsWith("ERROR:")) error(it.removePrefix("ERROR:"))
            }
        } finally {
            plain_prefs_string_free(result)
        }
    }

    actual fun set(key: String, valueJson: String) = memScoped {
        checkError(plain_prefs_set(key.cstr.ptr, valueJson.cstr.ptr))
    }

    actual fun remove(key: String) = memScoped {
        checkError(plain_prefs_remove(key.cstr.ptr))
    }

    private fun checkError(pointer: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?) {
        if (pointer != null) {
            val message = pointer.toKString()
            plain_prefs_string_free(pointer)
            error(message)
        }
    }
}
