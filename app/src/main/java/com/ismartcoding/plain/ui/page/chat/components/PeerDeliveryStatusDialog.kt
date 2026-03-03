package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DMessageStatusData

@Composable
fun PeerDeliveryStatusDialog(
    statusData: DMessageStatusData,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val failed = statusData.failedResults.firstOrNull()

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delivery_status),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = failed?.error ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        },
        confirmButton = {
            Button(
                enabled = failed != null,
                onClick = {
                    onRetry()
                    onDismiss()
                },
            ) {
                Text(text = stringResource(R.string.try_again))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        },
    )
}
