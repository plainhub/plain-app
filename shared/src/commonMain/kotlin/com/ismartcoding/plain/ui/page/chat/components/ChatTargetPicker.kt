package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.check
import com.ismartcoding.plain.i18n.send
import com.ismartcoding.plain.ui.base.VerticalSpace
import org.jetbrains.compose.resources.painterResource

/**
 * Multi-select chat target list used by the share sheet and the forward
 * dialog: local chat pinned first, then Channels / Devices sections, with a
 * circular confirm button that shows the selection count (and a spinner
 * while [sending]). Selection state is owned by the caller ([selectedIds]
 * holds [com.ismartcoding.plain.chat.data.ChatTarget.encodedToId] values).
 * The caller owns the top app bar.
 */
@Composable
fun ChatTargetPicker(
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    sending: Boolean = false,
) {
    val options = chatTargetOptions()
    val localOption = options.first { it.target.isLocal() }
    val channelOptions = options.filter { it.target.type == ChatTargetType.CHANNEL }
    val peerOptions = options.filter { !it.target.isLocal() && it.target.type == ChatTargetType.PEER }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { TargetRow(localOption, selectedIds.contains(localOption.target.encodedToId), enabled, onToggle) }
        items(channelOptions, key = { it.target.encodedToId }) {
            TargetRow(it, selectedIds.contains(it.target.encodedToId), enabled, onToggle)
        }
        items(peerOptions, key = { it.target.encodedToId }) {
            TargetRow(it, selectedIds.contains(it.target.encodedToId), enabled, onToggle)
        }
    }

    VerticalSpace(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        SendButton(count = selectedIds.size, enabled = selectedIds.isNotEmpty() && enabled && !sending, sending = sending, onClick = onConfirm)
    }
}

@Composable
private fun TargetRow(option: ChatTargetOption, selected: Boolean, enabled: Boolean, onToggle: (String) -> Unit) {
    val id = option.target.encodedToId
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent)
            .clickable(enabled = enabled) { onToggle(id) }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.subtitle.isNotEmpty()) {
                Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        CheckCircle(selected = selected)
    }
}

@Composable
private fun CheckCircle(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(width = 1.5.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SendButton(count: Int, enabled: Boolean, sending: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (enabled || sending) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (sending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.send),
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (count > 0 && !sending) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}
