package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.features.chat.models.LinkPreview
import com.ismartcoding.plain.ui.extensions.rememberFilePath
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.launchUrl
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatLinkPreview(linkPreview: LinkPreview, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .clickable { launchUrl(linkPreview.url) },
    ) {
        Column {
            val imageLocalPath = linkPreview.imageLocalPath
            val domain = linkPreview.domain
            val title = linkPreview.title
            val description = linkPreview.description
            if (!imageLocalPath.isNullOrEmpty() && linkPreview.imageWidth >= 200 && linkPreview.imageHeight >= 200) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = rememberFilePath(imageLocalPath),
                        contentDescription = stringResource(Res.string.link_preview_image),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (!domain.isNullOrEmpty()) {
                    Surface(modifier = Modifier.wrapContentSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(text = domain.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (!title.isNullOrEmpty()) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (!description.isNullOrEmpty()) {
                    Text(text = description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val showSmallIcon = (!imageLocalPath.isNullOrEmpty() &&
                            (linkPreview.imageWidth < 200 || linkPreview.imageHeight < 200)) ||
                            (imageLocalPath.isNullOrEmpty() && !linkPreview.imageUrl.isNullOrEmpty())
                    if (showSmallIcon) {
                        AsyncImage(
                            model = if (!imageLocalPath.isNullOrEmpty()) rememberFilePath(imageLocalPath) else linkPreview.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = linkPreview.siteName?.takeIf { it.isNotEmpty() } ?: linkPreview.url,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
