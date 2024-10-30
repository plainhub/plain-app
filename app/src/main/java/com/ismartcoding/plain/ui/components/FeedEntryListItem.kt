package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTag
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedEntryListItem(
    feedEntriesVM: FeedEntriesViewModel,
    index: Int,
    m: DFeedEntry,
    feed: DFeed?,
    tags: List<DTag>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickTag: (DTag) -> Unit
) {
    Row {
        if (feedEntriesVM.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = feedEntriesVM.selectedIds.contains(m.id), onCheckedChange = {
                feedEntriesVM.select(m.id)
            })
        }

        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = feedEntriesVM.selectedItem.value?.id == m.id || feedEntriesVM.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .weight(1f),
            color = Color.Unspecified,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = m.title,
                            style = MaterialTheme.typography.listItemTitle()
                        )
                    }
                    if (m.image.isNotEmpty()) {
                        HorizontalSpace(dp = 12.dp)
                        AsyncImage(
                            model = m.image,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentDescription = m.image,
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                VerticalSpace(dp = 8.dp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = arrayOf((index + 1).toString(), feed?.name ?: "", m.author, m.publishedAt.timeAgo()).filter {
                            it.isNotEmpty()
                        }.joinToString(" · "),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                    tags.forEach { tag ->
                        ClickableText(
                            text = AnnotatedString("#" + tag.name),
                            modifier = Modifier
                                .wrapContentHeight()
                                .align(Alignment.Bottom),
                            style = MaterialTheme.typography.listItemTag(),
                            onClick = {
                                onClickTag(tag)
                            }
                        )
                    }
                }
            }
        }
    }
}
