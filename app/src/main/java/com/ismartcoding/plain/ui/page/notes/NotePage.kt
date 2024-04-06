package com.ismartcoding.plain.ui.page.notes


import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text2.input.textAsFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.features.note.NoteHelper
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.MdEditor
import com.ismartcoding.plain.ui.base.mdeditor.MdEditorBottomAppBar
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.NoteViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun NotePage(
    navController: NavHostController,
    viewModel: NoteViewModel = viewModel(),
    mdEditorViewModel: MdEditorViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    val window = (view.context as Activity).window
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val insetsController = WindowCompat.getInsetsController(window, view)
    var id by remember {
        val v = navController.currentBackStackEntry?.arguments?.getString("id") ?: ""
        mutableStateOf(if (v == "create") "" else v)
    }
    val mdListState = rememberLazyListState()
    val editorScrollState = rememberScrollState()
    var shouldRequestFocus by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        viewModel.editMode = id.isEmpty()
        mdEditorViewModel.load(context)
        scope.launch(Dispatchers.IO) {
            if (id.isNotEmpty()) {
                val item = NoteHelper.getById(id)
                viewModel.content = item?.content ?: ""
                mdEditorViewModel.textFieldState.edit {
                    append(viewModel.content)
                    setSelection(0)
                }
            }
            mdEditorViewModel.textFieldState.textAsFlow().debounce(200)
                .collectLatest { t ->
                    val isNew = id.isEmpty()
                    val text = t.toString()
                    if (viewModel.content == text) {
                        return@collectLatest
                    }
                    scope.launch(Dispatchers.IO) {
                        id =
                            NoteHelper.addOrUpdateAsync(id) {
                                title = text.cut(100).replace("\n", "")
                                content = text
                                viewModel.content = text
                            }
                        if (isNew) {
                            val tagId = navController.currentBackStackEntry?.arguments?.getString("tagId") ?: ""
                            if (tagId.isNotEmpty()) {
                                // create note from tag items page.
                                TagHelper.addTagRelations(arrayListOf(TagRelationStub(id).toTagRelation(tagId, DataType.NOTE)))
                            }
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    LaunchedEffect(viewModel.editMode) {
        if (viewModel.editMode) {
            keyboardController?.show()
            scope.launch(Dispatchers.IO) {
                delay(500)
                coMain {
                    insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                    if (shouldRequestFocus) {
                        focusRequester.requestFocus()
                        shouldRequestFocus = false
                    }
                }
            }
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    PScaffold(
        navController,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        actions = {
            if (viewModel.editMode) {
                PIconButton(
                    icon = Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = stringResource(id = R.string.undo),
                    enabled = mdEditorViewModel.textFieldState.undoState.canUndo,
                    tint = MaterialTheme.colorScheme.onSurface
                ) {
                    mdEditorViewModel.textFieldState.undoState.undo()
                }
                PIconButton(
                    icon = Icons.AutoMirrored.Outlined.Redo,
                    contentDescription = stringResource(id = R.string.redo),
                    enabled = mdEditorViewModel.textFieldState.undoState.canRedo,
                    tint = MaterialTheme.colorScheme.onSurface
                ) {
                    mdEditorViewModel.textFieldState.undoState.redo()
                }
            }
            PIconButton(
                icon = if (viewModel.editMode) painterResource(id = R.drawable.ic_markdown) else Icons.Outlined.Edit,
                contentDescription = stringResource(if (viewModel.editMode) R.string.view else R.string.edit),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                viewModel.editMode = !viewModel.editMode
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.editMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                MdEditorBottomAppBar(mdEditorViewModel)
            }
        },
        content = {
            if (viewModel.editMode) {
                MdEditor(viewModel = mdEditorViewModel, editorScrollState, focusRequester = focusRequester)
            } else {
                LazyColumn(state = mdListState) {
                    item {
                        TopSpace()
                    }
                    item {
                        MarkdownText(
                            text = viewModel.content,
                            modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
                        )
                    }
                    item {
                        BottomSpace()
                    }
                }
            }

        },
    )
}
