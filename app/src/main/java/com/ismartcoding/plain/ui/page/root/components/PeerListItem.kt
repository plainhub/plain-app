package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.green
import com.ismartcoding.plain.ui.theme.grey
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    icon: Int,
    online: Boolean? = null,
    latestChat: DChat? = null,
) {
    Surface(
        modifier =
            modifier,
        color = Color.Unspecified,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                if (online != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(
                                if (online)
                                    MaterialTheme.colorScheme.green
                                else
                                    MaterialTheme.colorScheme.grey
                            )
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.listItemTitle(),
                        modifier = Modifier.weight(1f)
                    )
                    latestChat?.let { chat ->
                        Text(
                            text = chat.createdAt.timeAgo(),
                            style = MaterialTheme.typography.listItemSubtitle(),
                        )
                    }
                }
                VerticalSpace(dp = 8.dp)
                Text(
                    text = latestChat?.let { getMessagePreview(it) } ?: desc,
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun getMessagePreview(chat: DChat): String {
    return when (chat.content.type) {
        DMessageType.TEXT.value -> {
            val textMessage = chat.content.value as? DMessageText
            textMessage?.text?.take(50) ?: stringResource(R.string.text_message)
        }
        DMessageType.IMAGES.value -> {
            val imagesMessage = chat.content.value as? DMessageImages
            val count = imagesMessage?.items?.size ?: 0
            if (count > 1) {
                "$count ${stringResource(R.string.images)}"
            } else {
                stringResource(R.string.image)
            }
        }
        DMessageType.FILES.value -> {
            val filesMessage = chat.content.value as? DMessageFiles
            val count = filesMessage?.items?.size ?: 0
            if (count > 1) {
                "$count ${stringResource(R.string.files)}"
            } else {
                stringResource(R.string.file)
            }
        }
        else -> stringResource(R.string.message)
    }
}
