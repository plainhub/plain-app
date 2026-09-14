package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.nav.navigateSharedFolder
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Chat card for a shared folder/link message (MessageType.SHARE). Tapping the
 * card opens the native shared folder browser — identical for sender and
 * receiver since both hold the same `/s/<id>#<token>` link.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatShareItem(
    navController: NavHostController,
    m: VChat,
    isSelectMode: Boolean,
    onSelect: (String) -> Unit,
    onLongClick: () -> Unit,
) {
    val share = m.value as DMessageShare
    val expiresAt = share.expiresAt
    val isExpired = expiresAt != null && expiresAt < TimeHelper.now()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.cardBackgroundNormal)
            .combinedClickable(
                onClick = {
                    if (isSelectMode) {
                        onSelect(m.id)
                    } else {
                        navController.navigateSharedFolder(m.id)
                    }
                },
                onLongClick = {
                    if (isSelectMode) return@combinedClickable
                    onLongClick()
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .then(if (isExpired) Modifier.alpha(0.62f) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folders),
                    contentDescription = share.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = share.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shareSubtitle(share, isExpired),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isExpired) {
                        ExpiredBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun shareSubtitle(share: DMessageShare, isExpired: Boolean): String {
    val parts = mutableListOf(stringResource(Res.string.folder_card_items, share.itemCount))
    if (share.totalSize > 0) parts.add(share.totalSize.formatBytes())
    val expiry = share.expiresAt?.let {
        if (isExpired) null else stringResource(Res.string.share_expires_on, it.formatDateTime())
    }
    if (expiry != null) parts.add(expiry)
    return parts.joinToString(" · ")
}

@Composable
private fun ExpiredBadge() {
    Surface(
        modifier = Modifier.padding(start = 8.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = stringResource(Res.string.share_expired),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
