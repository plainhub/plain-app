package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioDialog(
    modifier: Modifier = Modifier,
    title: String = "",
    options: List<RadioDialogOption> = emptyList(),
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn {
                items(options) { option ->
                    PDialogRadioRow(selected = option.selected, onClick = {
                        option.onClick()
                        onDismissRequest()
                    }, text = option.text)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.close))
            }
        },
        dismissButton = {},
    )
}

data class RadioDialogOption(
    val text: String = "",
    val selected: Boolean = false,
    val onClick: () -> Unit = {},
)
