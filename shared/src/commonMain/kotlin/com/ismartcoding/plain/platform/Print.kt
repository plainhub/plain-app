package com.ismartcoding.plain.platform

import androidx.compose.ui.text.TextMeasurer

/**
 * Print [content] as a text document using the platform's print subsystem.
 *
 * On Android this delegates to the platform `PrintManager` and uses
 * [textMeasurer] to lay out the text into pages. On iOS this is a no-op
 * (UIActivityViewController with a PDF would be the equivalent).
 *
 * @param textMeasurer Compose text measurer used to paginate the content.
 * @param jobName      Name shown in the system print queue.
 * @param content      Plain text body to print.
 */
expect fun printText(textMeasurer: TextMeasurer, jobName: String, content: String)
