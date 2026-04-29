package com.ismartcoding.plain.ui.page

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.pathToAceMode
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.AceEditor
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.components.EditorData
import com.ismartcoding.plain.ui.models.TextFileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFilePage(
    navController: NavHostController,
    path: String,
    title: String,
    mediaId: String = "",
    type: String = TextFileType.DEFAULT.name,
    textFileVM: TextFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val darkTheme = LocalDarkTheme.current
    val isDarkTheme = DarkTheme.isDarkTheme(darkTheme)

    var isSaving by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isSaving) 360f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "save_rotation"
    )

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            textFileVM.loadConfigAsync(context)
            textFileVM.loadFileAsync(context, path, mediaId)
            textFileVM.isDataLoading.value = false
        }
    }

    if (textFileVM.showMoreActions.value) {
        ViewTextFileBottomSheet(textFileVM, path, textFileVM.file.value, onDeleted = {
            scope.launch {
                navController.navigateUp()
            }
        })
    }

    BackHandler(enabled = !textFileVM.readOnly.value) {
        textFileVM.exitEditMode()
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                title = title.ifEmpty { path.getFilenameFromPath() },
                navController = navController,
                navigationIcon = {
                    if (textFileVM.readOnly.value) {
                        NavigationBackIcon { navController.navigateUp() }
                    } else {
                        NavigationCloseIcon {
                            textFileVM.exitEditMode()
                        }
                    }
                },
                actions = {
                    TextFilePageActions(
                        textFileVM = textFileVM,
                        type = type,
                        path = path,
                        isSaving = isSaving,
                        rotation = rotation,
                        scope = scope,
                        context = context,
                        onSavingChanged = { isSaving = it },
                    )
                },
            )
        },
        content = { paddingValues ->
            // Use Box so the WebView renders in the background while the spinner
            // overlays it — avoids a layout-shift freeze when the spinner hides.
            Box(modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .fillMaxSize()) {
                if (!textFileVM.isDataLoading.value) {
                    AceEditor(
                        textFileVM, scope,
                        EditorData(
                            language = path.pathToAceMode(),
                            wrapContent = textFileVM.wrapContent.value,
                            isDarkTheme = isDarkTheme,
                            readOnly = textFileVM.readOnly.value,
                            gotoEnd = type == TextFileType.APP_LOG.name,
                            content = textFileVM.content.value
                        )
                    )
                }
                if (textFileVM.isDataLoading.value || !textFileVM.isEditorReady.value) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
    )
}

