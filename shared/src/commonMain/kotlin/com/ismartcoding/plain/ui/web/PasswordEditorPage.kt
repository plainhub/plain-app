package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PasswordEditorPage(
    state: PasswordSettingsState,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onGenerate: () -> Unit,
) {
    val configured = state.settings?.passwordConfigured == true
    val saving = state.operation == PasswordSettingsOperation.SAVE
    PScaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            PTopAppBar(
                title = stringResource(if (configured) Res.string.change_login_password else Res.string.set_login_password),
                onNavigateBack = onCancel,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                POutlinedButton(
                    text = stringResource(Res.string.cancel), onClick = onCancel,
                    modifier = Modifier.weight(1f), enabled = !state.busy,
                )
                PFilledButton(
                    text = stringResource(if (saving) Res.string.saving else Res.string.save_password),
                    onClick = onSave, modifier = Modifier.weight(1f),
                    enabled = state.canSave || saving, isLoading = saving,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.error) {
                PasswordSettingsError.SAVE -> PAlert(stringResource(Res.string.password_save_failed), AlertType.ERROR)
                PasswordSettingsError.GENERATE -> PAlert(stringResource(Res.string.password_generate_failed), AlertType.ERROR)
                else -> Unit
            }
            Text(
                text = stringResource(when {
                    saving -> Res.string.saving
                    state.generated -> Res.string.password_generated_unsaved
                    else -> Res.string.password_unsaved
                }),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(if (configured) Res.string.change_password_description else Res.string.set_password_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PTextField(
                readOnly = false, value = state.password, onValueChange = state::updatePassword,
                label = stringResource(if (configured) Res.string.new_login_password else Res.string.password),
                isPassword = true, enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                errorMessage = if (state.error == PasswordSettingsError.TOO_LONG) stringResource(Res.string.password_too_long) else "",
            )
            Text(
                text = stringResource(Res.string.password_length_advice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PTextField(
                readOnly = false, value = state.confirmation, onValueChange = state::updateConfirmation,
                label = stringResource(Res.string.confirm_login_password), isPassword = true, enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (state.canSave) onSave() }),
                errorMessage = if (state.error == PasswordSettingsError.MISMATCH) stringResource(Res.string.password_mismatch) else "",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PTextButton(
                    text = stringResource(Res.string.generate_password), onClick = onGenerate,
                    isLoading = state.operation == PasswordSettingsOperation.GENERATE,
                    enabled = !state.busy || state.operation == PasswordSettingsOperation.GENERATE,
                )
                if (state.generated) {
                    PTextButton(
                        text = stringResource(Res.string.copy), enabled = !state.busy,
                        onClick = {
                            setClipboardText("password", state.password, sensitive = true)
                            DialogHelper.showMessage(Res.string.copied)
                        },
                    )
                }
            }
            if (state.generated) {
                Text(stringResource(Res.string.generated_password_hint), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = stringResource(Res.string.password_write_only_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpace(8.dp)
        }
    }
}
