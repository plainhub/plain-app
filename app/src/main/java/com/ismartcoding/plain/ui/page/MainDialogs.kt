package com.ismartcoding.plain.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.ui.base.PToast
import com.ismartcoding.plain.ui.base.ToastEvent

@Composable
fun MainDialogs(
    loadingEvent: LoadingDialogEvent?,
    confirmEvent: ConfirmDialogEvent?,
    onDismissConfirm: () -> Unit,
    toastState: ToastEvent?,
    onDismissToast: () -> Unit,
) {
    loadingEvent?.let {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    confirmEvent?.let {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { if (it.title.isNotEmpty()) Text(it.title) },
            text = { Text(it.message) },
            confirmButton = {
                Button(onClick = { it.confirmButton.second(); onDismissConfirm() }) {
                    Text(it.confirmButton.first)
                }
            },
            dismissButton = {
                if (it.dismissButton != null) {
                    TextButton(onClick = { it.dismissButton.second(); onDismissConfirm() }) {
                        Text(it.dismissButton.first)
                    }
                }
            },
        )
    }
    toastState?.let { event ->
        PToast(message = event.message, type = event.type, onDismiss = onDismissToast)
    }
}
