package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.rustprefs.plain_prefs_open
import com.ismartcoding.plain.rustprefs.plain_prefs_string_free
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
internal actual object RustPrefsBridge {
    actual fun open(systemPath: String, userPath: String) = memScoped {
        checkError(plain_prefs_open(systemPath.cstr.ptr, userPath.cstr.ptr))
    }

    private fun checkError(pointer: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?) {
        if (pointer != null) {
            val message = pointer.toKString()
            plain_prefs_string_free(pointer)
            error(message)
        }
    }
}
