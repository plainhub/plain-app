@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ismartcoding.plain.ui.page.sharedfolder

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.share.SharedFileDto
import com.ismartcoding.plain.features.share.SharedInfoDto
import com.ismartcoding.plain.features.share.SharedLink
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.FileEntryThumb
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Info strip under the top bar: item count plus expiry. */
@Composable
internal fun MetaBanner(info: SharedInfoDto?) {
    if (info == null) return
    val parts = mutableListOf(stringResource(Res.string.folder_card_items, info.entries.size))
    val expiresAt = info.expiresAtInstant
    if (expiresAt != null) {
        val expired = expiresAt < TimeHelper.now()
        val dateText = expiresAt.formatDateTime()
        parts.add(
            if (expired) dateText
            else stringResource(Res.string.share_expires_on, dateText),
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.cardBackgroundNormal,
    ) {
        Text(
            text = parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Load-failure strip with a retry action. */
@Composable
internal fun ErrorBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.cannot_load_share),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            POutlinedButton(
                text = stringResource(Res.string.retry),
                onClick = onRetry,
                type = ButtonType.DANGER,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** FilesPage-style file row: rounded card, shared thumbnail, name + size, select checkbox. */
@Composable
internal fun EntryRow(
    entry: SharedFileDto,
    link: SharedLink,
    urlToken: String,
    selectMode: Boolean,
    selected: Boolean,
    progress: Float?,
    previewLoading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (selectMode && selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.cardBackgroundNormal
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectMode) {
                Checkbox(checked = selected, onCheckedChange = null)
                Box(modifier = Modifier.size(8.dp))
            }
            FileEntryThumb(
                name = entry.name,
                path = "",
                isDir = entry.isDir,
                isMedia = SharedThumbCache.isThumbable(entry),
                remoteThumb = { SharedThumbCache.thumbPath(link, urlToken, entry) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!entry.isDir && entry.size > 0) {
                    Text(
                        text = entry.size.formatBytes(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!selectMode && progress == null && !previewLoading) {
                PIconButton(
                    icon = Res.drawable.download,
                    contentDescription = stringResource(Res.string.download),
                    tint = MaterialTheme.colorScheme.primary,
                ) { onDownload() }
            }
        }
        when {
            progress != null -> LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            previewLoading -> LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
