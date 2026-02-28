package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData

/**
 * Dialog that shows per-member delivery status for a channel message.
 *
 * - Delivered members are shown with a green check.
 * - Failed members are shown with a red warning icon, their truncated error message,
 *   and a pre-checked checkbox. Users may deselect members they do not want to retry.
 * - "Resend selected" triggers [onResend] with the list of selected peer IDs.
 */
@Composable
fun DeliveryStatusDialog(
    statusData: DMessageStatusData,
    onResend: (peerIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Pre-select all failed members by default
    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(statusData.failedResults.map { it.peerId })
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.delivery_status),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(
                        R.string.delivery_status_summary,
                        statusData.deliveredCount,
                        statusData.total,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Delivered members first
                items(statusData.deliveredResults, key = { "d_${it.peerId}" }) { result ->
                    DeliveryResultRow(result = result, isSelected = false, onToggle = null)
                }
                // Failed members
                items(statusData.failedResults, key = { "f_${it.peerId}" }) { result ->
                    DeliveryResultRow(
                        result = result,
                        isSelected = selectedIds.contains(result.peerId),
                        onToggle = {
                            if (selectedIds.contains(result.peerId)) {
                                selectedIds.remove(result.peerId)
                            } else {
                                selectedIds.add(result.peerId)
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedIds.isNotEmpty(),
                onClick = {
                    onResend(selectedIds.toList())
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.resend_selected))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun DeliveryResultRow(
    result: DMessageDeliveryResult,
    isSelected: Boolean,
    onToggle: (() -> Unit)?,
) {
    val isSuccess = result.error == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isSuccess) {
            // Green check icon aligned with first line
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 1.dp),
            )
        } else {
            // Checkbox (pre-selected) for failed members
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle?.invoke() },
                modifier = Modifier.size(18.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Text(
                text = result.peerName.ifEmpty { result.peerId },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isSuccess) {
                Text(
                    text = result.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
