package com.ismartcoding.plain.platform

expect fun setClipboardText(label: String, text: String, sensitive: Boolean = false)

expect fun getClipboardText(): String?
