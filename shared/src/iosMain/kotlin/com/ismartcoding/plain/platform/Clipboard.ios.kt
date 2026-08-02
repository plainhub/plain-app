package com.ismartcoding.plain.platform

import platform.UIKit.UIPasteboard

actual fun setClipboardText(label: String, text: String) {
    UIPasteboard.generalPasteboard.string = text
}

actual fun getClipboardText(): String? {
    return UIPasteboard.generalPasteboard.string
}
