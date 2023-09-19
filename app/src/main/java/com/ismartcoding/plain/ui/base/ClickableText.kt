package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit,
    onDoubleClick: () -> Unit,
    doubleClickTimeoutMillis: Long = 300
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val lastClickTime = remember { mutableStateOf(0L) }

    val pressIndicator = Modifier.pointerInput(onClick) {
        detectTapGestures { pos ->
            val currentTime = System.currentTimeMillis()
            val layout = layoutResult.value
            if (layout != null) {
                val offset = layout.getOffsetForPosition(pos)
                val timeSinceLastClick = currentTime - lastClickTime.value
                if (timeSinceLastClick <= doubleClickTimeoutMillis) {
                    // Double-click detected
                    onDoubleClick()
                } else {
                    // Single click
                    onClick(offset)
                }
                lastClickTime.value = currentTime
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier.then(pressIndicator),
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout(it)
        }
    )
}
