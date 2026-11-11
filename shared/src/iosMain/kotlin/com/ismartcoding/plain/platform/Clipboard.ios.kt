package com.ismartcoding.plain.platform

import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionLocalOnly
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

internal const val SENSITIVE_PASTEBOARD_TYPE = "org.nspasteboard.ConcealedType"

actual fun setClipboardText(label: String, text: String, sensitive: Boolean) {
    if (sensitive) {
        UIPasteboard.generalPasteboard.setItems(
            listOf(mapOf("public.utf8-plain-text" to text, SENSITIVE_PASTEBOARD_TYPE to "true")),
            options = mapOf(
                UIPasteboardOptionLocalOnly to true,
                UIPasteboardOptionExpirationDate to NSDate.dateWithTimeIntervalSinceNow(120.0),
            ),
        )
    } else {
        UIPasteboard.generalPasteboard.string = text
    }
}

actual fun getClipboardText(): String? {
    return UIPasteboard.generalPasteboard.string
}
