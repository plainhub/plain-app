package com.ismartcoding.plain.lib.markdown.utils

internal actual fun platformLog(tag: String, message: String) {
    // No-op on native (iOS) targets.
}
