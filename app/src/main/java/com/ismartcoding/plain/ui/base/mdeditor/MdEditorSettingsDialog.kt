package com.ismartcoding.plain.ui.base.mdeditor
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.preferences.EditorShowLineNumbersPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MdEditorSettingsDialog(
    mdEditorVM: MdEditorViewModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            mdEditorVM.showSettings.value = false
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.close),
                buttonSize = ButtonSize.MEDIUM,
                onClick = {
                    mdEditorVM.showSettings.value = false
                },
            )
        },
        title = {
            Text(text = stringResource(Res.string.editor_settings),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                PDialogListItem(
                    title = stringResource(Res.string.show_line_numbers),
                ) {
                    PSwitch(
                        activated = mdEditorVM.showLineNumbers.value,
                    ) {
                        mdEditorVM.showLineNumbers.value = it
                        scope.launch(Dispatchers.IO) {
                            EditorShowLineNumbersPreference.putAsync(it)
                        }
                    }
                }
                PDialogListItem(
                    title = stringResource(Res.string.wrap_content),
                ) {
                    PSwitch(
                        activated = mdEditorVM.wrapContent.value,
                    ) {
                        mdEditorVM.wrapContent.value = it
                        scope.launch(Dispatchers.IO) {
                            EditorWrapContentPreference.putAsync(it)
                        }
                    }
                }
//                PDialogListItem(
//                    title = stringResource(Res.string.syntax_highlight),
//                ) {
//                    PSwitch(
//                        activated = viewModel.syntaxHighLight,
//                    ) {
//                        viewModel.syntaxHighLight = it
//                        scope.launch(Dispatchers.IO) {
//                            EditorSyntaxHighlightPreference.putAsync(it)
//                        }
//                    }
//                }
            }
        })
}