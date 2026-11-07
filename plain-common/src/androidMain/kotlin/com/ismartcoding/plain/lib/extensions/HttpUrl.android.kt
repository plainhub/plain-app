package com.ismartcoding.plain.lib.extensions

import java.net.MalformedURLException
import java.net.URL

internal actual fun String.hasUrlHost(): Boolean = try {
    URL(this).host.isNotEmpty()
} catch (_: MalformedURLException) {
    false
}
