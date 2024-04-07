package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.ismartcoding.plain.ui.models.VClickText

@Composable
fun PClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onClick: (Int) -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator =
        Modifier.pointerInput(onClick) {
            detectTapGestures(onDoubleTap = { onDoubleClick() }, onTap = {
                val layout = layoutResult.value
                if (layout != null) {
                    val offset = layout.getOffsetForPosition(it)
                    onClick(offset)
                }
            }, onLongPress = { onLongClick() })
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
        },
    )
}


@Composable
fun PClickableText(
    text: String,
    clickTexts: List<VClickText>,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
) {
    val fullText = text.linkify(clickTexts)
    androidx.compose.foundation.text.ClickableText(
        text = fullText,
        modifier = modifier,
        style = style,
        onClick = { position ->
            fullText.clickAt(position, clickTexts)
        },
    )
}