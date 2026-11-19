package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PFilledButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioDialog(
    modifier: Modifier = Modifier,
    title: String = "",
    options: List<RadioDialogOption> = emptyList(),
    compact: Boolean = false,
    description: String = "",
    confirmText: String = stringResource(Res.string.confirm),
    onConfirm: (() -> Unit)? = null,
    dismissOnSelect: Boolean = onConfirm == null,
    onDismissRequest: () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 4.dp),
            ) {
                if (description.isNotEmpty()) item {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                items(options) { option ->
                    PDialogRadioRow(
                        selected = option.selected,
                        onClick = {
                            option.onClick()
                            if (dismissOnSelect) onDismissRequest()
                        },
                        text = option.text,
                        supportingText = option.supportingText,
                        modifier = if (compact) Modifier.heightIn(min = 48.dp) else Modifier,
                    )
                }
            }
        },
        confirmButton = {
            if (onConfirm == null) {
                PTextButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = onDismissRequest)
            } else {
                PFilledButton(text = confirmText, onClick = onConfirm)
            }
        },
        dismissButton = {
            if (onConfirm != null) PTextButton(text = stringResource(Res.string.cancel), onClick = onDismissRequest)
        },
    )
}
