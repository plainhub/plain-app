package com.ismartcoding.plain.lib.extensions

import com.ismartcoding.plain.platform.appDir

fun String.getFinalPath(): String {
    val dir = appDir()
    if (this.startsWith("app://", true)) {
        return dir + "/" + this.substring("app://".length)
    }

    if (this.startsWith("fid:", true)) {
        val hash = this.substring("fid:".length)
        return "$dir/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$hash"
    }

    return this
}
