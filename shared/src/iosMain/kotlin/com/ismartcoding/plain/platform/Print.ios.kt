package com.ismartcoding.plain.platform

import androidx.compose.ui.text.TextMeasurer

actual fun printText(textMeasurer: TextMeasurer, jobName: String, content: String) {
    // iOS has no equivalent UIPrintInteractionController for plain text from a
    // Compose context without a UIPrintFormatter; left as a no-op for now.
}
