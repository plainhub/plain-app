package com.ismartcoding.plain.platform

import androidx.compose.ui.text.TextMeasurer
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.PrintHelper

actual fun printText(textMeasurer: TextMeasurer, jobName: String, content: String) {
    PrintHelper.printText(appContext, textMeasurer, jobName, content)
}
