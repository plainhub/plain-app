package com.ismartcoding.plain.ui.page

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.codeeditor.CodeEditor
import com.ismartcoding.plain.ui.components.codeeditor.EditorLoadState
import com.ismartcoding.plain.ui.models.TextFileViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFilePage(
    navController: NavHostController,
    path: String,
    title: String,
    mediaId: String = "",
    type: String = TextFileType.DEFAULT.name,
    textFileVM: TextFileViewModel = viewModel { TextFileViewModel() },
) {
    var isSaving by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    // Close affordances in edit mode (X / back) exit edit mode, not the page.
    fun requestClose() {
        if (textFileVM.controller.isDirty.value) showDiscardConfirm = true else textFileVM.exitEditMode(discard = false)
    }
    val rotation by animateFloatAsState(
        targetValue = if (isSaving) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "save_rotation",
    )

    LaunchedEffect(Unit) {
        textFileVM.loadConfigAsync()
        textFileVM.loadFileAsync(path, mediaId, gotoEnd = type == TextFileType.APP_LOG.name)
    }

    if (textFileVM.showMoreActions.value) {
        ViewTextFileBottomSheet(textFileVM, path, textFileVM.file.value, onDeleted = {
            navController.navigateUp()
        })
    }

    PBackHandler(enabled = !textFileVM.controller.readOnly.value) {
        requestClose()
    }

    if (showDiscardConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(Res.string.discard_changes)) },
            text = { Text(stringResource(Res.string.discard_changes_text)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDiscardConfirm = false
                    textFileVM.exitEditMode(discard = true)
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = title.ifEmpty { path.getFilenameFromPath() },
                navController = navController,
                navigationIcon = {
                    if (textFileVM.controller.readOnly.value) {
                        NavigationBackIcon { navController.navigateUp() }
                    } else {
                        NavigationCloseIcon { requestClose() }
                    }
                },
                actions = {
                    TextFilePageActions(
                        textFileVM = textFileVM,
                        type = type,
                        path = path,
                        isSaving = isSaving,
                        rotation = rotation,
                        onSavingChanged = { isSaving = it },
                    )
                },
            )
        },
        content = { paddingValues ->
            val loadState = textFileVM.controller.loadState.value
            Box(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .fillMaxSize(),
            ) {
                if (loadState is EditorLoadState.Error) {
                    Text(
                        text = loadState.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                } else {
                    if (loadState is EditorLoadState.Ready) {
                        CodeEditor(
                            controller = textFileVM.controller,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        },
    )
}
