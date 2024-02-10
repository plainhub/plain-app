package com.ismartcoding.plain.ui.components.chat

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.ui.base.ClickableText
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt
import com.ismartcoding.plain.ui.models.VChat

@Composable
fun ChatText(
    context: Context,
    focusManager: FocusManager,
    m: VChat,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
    ) {
    val text =
        (m.value as DMessageText).text.linkify(
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    ClickableText(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        onClick = { position ->
            focusManager.clearFocus()
            text.urlAt(context, position)
        },
        onDoubleClick = onDoubleClick,
        onLongClick = onLongClick
    )
}
