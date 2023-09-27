package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            content = {
                Column(
                    Modifier.padding(vertical = 16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false),
        )
    }
}
