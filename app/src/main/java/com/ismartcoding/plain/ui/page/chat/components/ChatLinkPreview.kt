package com.ismartcoding.plain.ui.page.chat.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ismartcoding.lib.extensions.getFinalPath
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DLinkPreview
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun ChatLinkPreview(linkPreview: DLinkPreview, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkPreview.url))) },
    ) {
        Column {
            if (!linkPreview.imageLocalPath.isNullOrEmpty() && linkPreview.imageWidth >= 200 && linkPreview.imageHeight >= 200) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = linkPreview.imageLocalPath.getFinalPath(context),
                        contentDescription = stringResource(R.string.link_preview_image),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                if (!linkPreview.domain.isNullOrEmpty()) {
                    Surface(modifier = Modifier.wrapContentSize(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(text = linkPreview.domain.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (!linkPreview.title.isNullOrEmpty()) {
                    Text(text = linkPreview.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (!linkPreview.description.isNullOrEmpty()) {
                    Text(text = linkPreview.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val showSmallIcon = (!linkPreview.imageLocalPath.isNullOrEmpty() &&
                            (linkPreview.imageWidth < 200 || linkPreview.imageHeight < 200)) ||
                            (linkPreview.imageLocalPath.isNullOrEmpty() && !linkPreview.imageUrl.isNullOrEmpty())
                    if (showSmallIcon) {
                        AsyncImage(
                            model = if (!linkPreview.imageLocalPath.isNullOrEmpty()) linkPreview.imageLocalPath.getFinalPath(context) else linkPreview.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = linkPreview.siteName?.takeIf { it.isNotEmpty() } ?: linkPreview.url,
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
            }
        }
    }
}
