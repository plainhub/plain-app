package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.platform.appDir

fun String.getFinalPath(): String {
    val dir = appDir()
    if (this.startsWith("app://", true)) {
        return dir + "/" + this.substring("app://".length)
    }

    if (this.startsWith("fid:", true)) {
        return com.ismartcoding.plain.helpers.AppFileStore.realPathFromId(this.substring(4))
    }

    return this
}

/**
 * Resolve a stored `app_files.real_path` value to an absolute filesystem path.
 *
 * New rows store the relative portion (`{aa}/{bb}/{name}`) to avoid repeating
 * the platform-specific `appDir()` prefix on every row; legacy rows may still
 * hold absolute paths. Absolute paths (starting with `/`) and empty strings
 * are returned unchanged.
 */
fun String.resolveAppFileRealPath(): String {
    if (this.isEmpty() || this.startsWith("/")) return this
    return com.ismartcoding.plain.api.RustContentApi.directory + "/" + this
}
