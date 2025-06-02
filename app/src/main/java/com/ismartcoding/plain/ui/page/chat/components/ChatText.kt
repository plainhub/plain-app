package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.ui.base.PClickableText
import com.ismartcoding.plain.ui.base.linkify
import com.ismartcoding.plain.ui.base.urlAt
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.select

@Composable
fun ChatText(
    context: Context,
    chatVM: ChatViewModel,
    focusManager: FocusManager,
    m: VChat,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val messageText = m.value as DMessageText
    val text = messageText.text.linkify()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        PClickableText(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            onClick = { position ->
                if (chatVM.selectMode.value) {
                    chatVM.select(m.id)
                } else {
                    focusManager.clearFocus()
                    text.urlAt(context, position)
                }
            },
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick
        )

        if (messageText.linkPreviews.isNotEmpty()) {
            messageText.linkPreviews.forEach { linkPreview ->
                ChatLinkPreview(
                    linkPreview = linkPreview,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}
