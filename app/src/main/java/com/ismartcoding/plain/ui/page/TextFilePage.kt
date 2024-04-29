package com.ismartcoding.plain.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.pathToAceMode
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.helpers.AppLogHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.base.AceEditor
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFilePage(
    navController: NavHostController,
    path: String,
    title: String,
    mediaStoreId: String = "",
    type: String = TextFileType.DEFAULT.name,
    viewModel: TextFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadConfigAsync(context)
            viewModel.loadFileAsync(context, path, mediaStoreId)
            viewModel.isDataLoading.value = false
        }
    }

    if (viewModel.showMoreActions.value) {
        ViewTextFileBottomSheet(viewModel, path, viewModel.file.value, onDeleted = {
            scope.launch {
                navController.popBackStack()
            }
        })
    }

    BackHandler(enabled = !viewModel.readOnly.value) {
        viewModel.exitEditMode()
    }

    PScaffold(
        navController,
        topBarTitle = title.ifEmpty { path.getFilenameFromPath() },
        navigationIcon = {
            if (viewModel.readOnly.value) {
                NavigationBackIcon {
                    navController.popBackStack()
                }
            } else {
                NavigationCloseIcon {
                    viewModel.exitEditMode()
                }
            }
        },
        actions = {
            if (!viewModel.isEditorReady.value) {
                return@PScaffold
            }
            if (viewModel.readOnly.value) {
                if (type != TextFileType.APP_LOG.name) {
                    PIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        viewModel.enterEditMode()
                    }
                }
            } else {
                PIconButton(
                    icon = Icons.Outlined.Save,
                    contentDescription = stringResource(R.string.save),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    scope.launch {
                        DialogHelper.showLoading()
                        withIO { File(path).writeText(viewModel.content.value) }
                        context.scanFileByConnection(path)
                        DialogHelper.hideLoading()
                        DialogHelper.showMessage(R.string.saved)
                    }
                }
            }
            if (setOf(TextFileType.APP_LOG.name, TextFileType.CHAT.name).contains(type)) {
                PIconButton(
                    icon = Icons.AutoMirrored.Outlined.WrapText,
                    contentDescription = stringResource(R.string.wrap_content),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    viewModel.toggleWrapContent(context)
                }
                PIconButton(
                    icon = Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    if (type == TextFileType.APP_LOG.name) {
                        AppLogHelper.export(context)
                    } else if (type == TextFileType.CHAT.name) {
                        ShareHelper.shareFile(context, File(path))
                    }
                }
            } else {
                ActionButtonMore {
                    viewModel.showMoreActions.value = true
                }
            }
        },
        content = {
            if (viewModel.isDataLoading.value || !viewModel.isEditorReady.value) {
                NoDataColumn(loading = true)
            }
            if (viewModel.isDataLoading.value) {
                return@PScaffold
            }
            AceEditor(
                viewModel, scope,
                EditorData(
                    language = path.pathToAceMode(),
                    wrapContent = viewModel.wrapContent.value,
                    isDarkTheme = isDarkTheme,
                    readOnly = viewModel.readOnly.value,
                    gotoEnd = type == TextFileType.APP_LOG.name,
                    content = viewModel.content.value
                )
            )
        },
    )
}

