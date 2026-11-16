package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.ismartcoding.plain.features.web.WebHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.ChatTextFormat
import com.ismartcoding.plain.ui.editor.markdown.MarkdownText
import com.ismartcoding.plain.ui.base.PClickableText
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.components.urlAt
import com.ismartcoding.plain.ui.chat.VChat

@Composable
fun ChatText(
    focusManager: FocusManager,
    m: VChat,
    message: ChatMessage,
    body: String,
    isSelectMode: Boolean,
    onSelect: (String) -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    if (message.format == ChatTextFormat.MARKDOWN) {
        val openLink: (String) -> Unit = { url -> if (isChatMarkdownLink(url)) WebHelper.open(url) }
        CompositionLocalProvider(LocalUriHandler provides object : UriHandler {
            override fun openUri(uri: String) = openLink(uri)
        }) {
            MarkdownText(body, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), loadImages = false, openLink = openLink)
        }
        return
    }
    val text = body.linkify()

    PClickableText(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        onClick = { position ->
            if (isSelectMode) {
                onSelect(m.id)
            } else {
                focusManager.clearFocus()
                text.urlAt(position)
            }
        },
        onDoubleClick = onDoubleClick,
        onLongClick = onLongClick
    )

    if (message.linkPreviews.isNotEmpty()) {
        message.linkPreviews.forEach { linkPreview ->
            ChatLinkPreview(
                linkPreview = linkPreview,
            )
        }
    }
}

internal fun isChatMarkdownLink(url: String): Boolean {
    if (url.any { it.isWhitespace() || it.isISOControl() || it == '\\' }) return false
    if (url.startsWith("mailto:", ignoreCase = true)) {
        return url.substringAfter(':').substringBefore('?').contains('@')
    }
    if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) return false
    val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
    return host.isNotEmpty() && '@' !in host
}
