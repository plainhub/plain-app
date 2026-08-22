package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.combineBitmapGrid
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

@Composable
fun MediaSidebarBucketItem(
    m: DMediaBucket,
    isMedia: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val bitmapResult = remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(m.id, m.topItems) {
        bitmapResult.value = withContext(Dispatchers.Default) {
            combineBitmapGrid(m.topItems, 150)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMedia && m.topItems.isNotEmpty()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalPlatformContext.current)
                        .data(bitmapResult.value)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.folder),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }

        HorizontalSpace(12.dp)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = m.name,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            VerticalSpace(2.dp)
            Text(
                text = m.size.formatBytes(),
                style = MaterialTheme.typography.listItemSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalSpace(8.dp)
        Text(
            text = m.itemCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
