package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.extensions.getText
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemDescription
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedListItem(
    feedsVM: FeedsViewModel,
    m: DFeed,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row {
        if (feedsVM.selectMode.value) {
            HorizontalSpace(dp = 16.dp)
            Checkbox(checked = feedsVM.selectedIds.contains(m.id), onCheckedChange = {
                feedsVM.select(m.id)
            })
        }

        Surface(
            modifier =
            PlainTheme
                .getCardModifier(selected = feedsVM.selectedItem.value?.id == m.id || feedsVM.selectedIds.contains(m.id))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .weight(1f),
            color = Color.Unspecified,
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = m.name + " (${m.count})",
                        style = MaterialTheme.typography.listItemTitle(),
                    )
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = m.url,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.listItemDescription(),
                    )
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = stringResource(id = R.string.auto_fetch_full_content) + ": " + m.fetchContent.getText(),
                        style = MaterialTheme.typography.listItemSubtitle(),
                    )
                }
            }
        }
    }
}
