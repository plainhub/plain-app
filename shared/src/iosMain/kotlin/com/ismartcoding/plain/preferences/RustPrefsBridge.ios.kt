package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.rustprefs.plain_prefs_open
import com.ismartcoding.plain.rustprefs.plain_prefs_remove_system
import com.ismartcoding.plain.rustprefs.plain_prefs_remove_user
import com.ismartcoding.plain.rustprefs.plain_prefs_set_system
import com.ismartcoding.plain.rustprefs.plain_prefs_set_user
import com.ismartcoding.plain.rustprefs.plain_prefs_system_snapshot
import com.ismartcoding.plain.rustprefs.plain_prefs_user_snapshot
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

    actual fun systemSnapshot(): String = snapshot(plain_prefs_system_snapshot())

    actual fun userSnapshot(): String = snapshot(plain_prefs_user_snapshot())

    private fun snapshot(result: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?): String {
        val value = result ?: error("Rust preferences returned no snapshot")
        try {
            return value.toKString().also {
                if (it.startsWith("ERROR:")) error(it.removePrefix("ERROR:"))
            }
        } finally {
            plain_prefs_string_free(value)
        }
    }

    actual fun setSystem(key: String, valueJson: String) = memScoped {
        checkError(plain_prefs_set_system(key.cstr.ptr, valueJson.cstr.ptr))
    }

    actual fun setUser(key: String, valueJson: String) = memScoped {
        checkError(plain_prefs_set_user(key.cstr.ptr, valueJson.cstr.ptr))
    }

    actual fun removeSystem(key: String) = memScoped {
        checkError(plain_prefs_remove_system(key.cstr.ptr))
    }

    actual fun removeUser(key: String) = memScoped {
        checkError(plain_prefs_remove_user(key.cstr.ptr))
    }

    private fun checkError(pointer: kotlinx.cinterop.CPointer<kotlinx.cinterop.ByteVar>?) {
        if (pointer != null) {
            val message = pointer.toKString()
            plain_prefs_string_free(pointer)
            error(message)
        }
    }
}
