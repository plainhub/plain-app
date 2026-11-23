package com.ismartcoding.plain.ui.media

import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundActive
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaFolderGridItem(
    modifier: Modifier = Modifier,
    m: MediaBucket,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) MaterialTheme.colorScheme.cardBackgroundActive else Color.Transparent)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                MediaThumbnailGrid(
                    items = m.coverItems,
                    contentDescription = m.name,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            VerticalSpace(16.dp)

            Text(
                text = m.name,
                style = MaterialTheme.typography.listItemTitle(),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            VerticalSpace(4.dp)
            var subtitle = pluralStringResource(Res.plurals.items, m.itemCount, m.itemCount)
            if (m.size > 0) {
                subtitle += " • " + m.size.formatBytes()
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.listItemSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
