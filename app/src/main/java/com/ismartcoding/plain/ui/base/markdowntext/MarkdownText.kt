package com.ismartcoding.plain.ui.base.markdowntext

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.extensions.markdown

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    truncateOnTextOverflow: Boolean = false,
    isTextSelectable: Boolean = true,
    style: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    previewerState: MediaPreviewerState,
) {
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val linkTextColor = MaterialTheme.colorScheme.primary
    var mText by remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier,
        factory = { factoryContext ->
            TextView(factoryContext).apply {
                setLinkTextColor(linkTextColor.toArgb())

                setTextIsSelectable(isTextSelectable)

                movementMethod = LinkMovementMethod.getInstance()

                if (truncateOnTextOverflow) enableTextOverflow()
            }
        },
        update = { textView ->
            if (text != mText) {
                mText = text
                with(textView) {
                    applyTextColor(style.color.takeOrElse { defaultColor }.toArgb())
                    applyFontSize(style)
                    applyLineHeight(style)
                    applyLineSpacing(style)
                    applyTextDecoration(style)

                    with(style) {
                        applyTextAlign(textAlign)
                        fontStyle?.let { applyFontStyle(it) }
                        fontWeight?.let { applyFontWeight(it) }
                    }
                }
                textView.markdown(text, previewerState)
            }
        }
    )
}
