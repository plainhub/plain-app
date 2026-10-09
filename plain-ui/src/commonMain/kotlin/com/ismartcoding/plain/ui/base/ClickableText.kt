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
import com.ismartcoding.plain.ui.models.VClickText

@Composable
fun PClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    onClick: ((Int) -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val pressIndicator = if (onClick != null || onDoubleClick != null || onLongClick != null) {
        Modifier.pointerInput(onClick, onDoubleClick, onLongClick) {
            detectTapGestures(
                onDoubleTap = onDoubleClick?.let { callback -> { _ -> callback() } },
                onTap = onClick?.let { callback -> { position ->
                    layoutResult.value?.let { callback(it.getOffsetForPosition(position)) }
                } },
                onLongPress = onLongClick?.let { callback -> { _ -> callback() } },
            )
        }
    } else Modifier

    BasicText(
        text = text,
        modifier = modifier.then(pressIndicator),
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = {
            layoutResult.value = it
            onTextLayout?.invoke(it)
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
    PClickableText(
        text = fullText,
        modifier = modifier,
        style = style,
        onClick = { position ->
            fullText.clickAt(position, clickTexts)
        },
    )
}
