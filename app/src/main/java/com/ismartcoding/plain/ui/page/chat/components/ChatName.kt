package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.extensions.formatTime
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.theme.secondaryTextColor

@Composable
fun ChatName(
    m: VChat,
    onRetry: (() -> Unit)? = null,
    onShowDeliveryDetails: ((DMessageStatusData) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                m.fromId == "local" -> getString(R.string.local_chat)
                m.fromId == "me" -> getString(R.string.me)
                else -> ChatCacheManager.peerNamesCache[m.fromId] ?: getString(R.string.unknown)
            },

            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(end = 4.dp),
        )

        Text(
            text = m.createdAt.formatTime(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.secondaryTextColor),
        )

        // Show status indicator based on message status
        when {
            m.status == "pending" -> {
                HorizontalSpace(4.dp)
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Channel message with per-member delivery data — show "X/N" badge
            m.fromId == "me" && m.statusData != null && m.statusData.total > 0 -> {
                val statusData = m.statusData
                val isAllFailed = statusData.allFailed
                val badgeColor = if (isAllFailed) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.tertiary

                HorizontalSpace(6.dp)
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onShowDeliveryDetails?.invoke(statusData) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isAllFailed) {
                            Icon(
                                painter = painterResource(R.drawable.rotate_ccw),
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(10.dp),
                            )
                            HorizontalSpace(3.dp)
                        }
                        Text(
                            text = statusData.deliveryLabel(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }

            // Peer / no-leader failure — show legacy retry button
            m.fromId == "me" && m.status == "failed" -> {
                if (onRetry != null) {
                    HorizontalSpace(4.dp)
                    PIconButton(
                        icon = R.drawable.rotate_ccw,
                        contentDescription = stringResource(R.string.try_again),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    ) {
                        onRetry()
                    }
                }
            }
        }
    }
}
