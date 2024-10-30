package com.ismartcoding.plain.ui.page.notes


import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.cut
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.base.ActionButtonTags
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.MdEditor
import com.ismartcoding.plain.ui.base.mdeditor.MdEditorBottomAppBar
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.NoteViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.tags.SelectTagsDialog
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
fun NotePage(
    navController: NavHostController,
    initId: String,
    tagId: String,
    notesVM: NotesViewModel,
    tagsVM: TagsViewModel,
    noteVM: NoteViewModel = viewModel(),
    mdEditorVM: MdEditorViewModel = viewModel()
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
        mutableStateOf(initId)
    }
    val previewerState = rememberPreviewerState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val mdListState = rememberLazyListState()
    val editorScrollState = rememberScrollState()
    var shouldRequestFocus by remember {
        mutableStateOf(true)
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { !noteVM.editMode })

    val tagIds = tagsMapState[id]?.map { it.tagId } ?: emptyList()
    LaunchedEffect(Unit) {
        tagsVM.dataType.value = DataType.NOTE
        noteVM.editMode = id.isEmpty()
        mdEditorVM.load(context)
        scope.launch(Dispatchers.IO) {
            if (id.isNotEmpty()) {
                val item = NoteHelper.getById(id)
                noteVM.item.value = item
                noteVM.content = item?.content ?: ""
                mdEditorVM.textFieldState.edit {
                    append(noteVM.content)
                    setSelection(0)
                }
            }
            snapshotFlow { mdEditorVM.textFieldState.text }.debounce(200)
                .collectLatest { t ->
                    val isNew = id.isEmpty()
                    val text = t.toString()
                    if (noteVM.content == text) {
                        return@collectLatest
                    }
                    scope.launch(Dispatchers.IO) {
                        val newItem =
                            NoteHelper.addOrUpdateAsync(id) {
                                title = text.cut(250).replace("\n", "")
                                content = text
                                noteVM.content = text
                            }
                        id = newItem.id
                        if (isNew) {
                            if (tagId.isNotEmpty()) {
                                // create note from tag items page.
                                TagHelper.addTagRelations(arrayListOf(TagRelationStub(id).toTagRelation(tagId, DataType.NOTE)))
                            }
                            tagsVM.loadAsync(setOf(id))
                        }
                        notesVM.updateItem(newItem)
                    }
                }

        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    BackHandler(previewerState.visible) {
        scope.launch {
            previewerState.close()
        }
    }

    LaunchedEffect(noteVM.editMode) {
        if (noteVM.editMode) {
            keyboardController?.show()
            if (shouldRequestFocus) {
                scope.launch(Dispatchers.IO) {
                    delay(500)
                    coMain {
                        focusRequester.requestFocus()
                        shouldRequestFocus = false
                    }
                }
            }
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    SideEffect {
        if (noteVM.editMode) {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    if (noteVM.showSelectTagsDialog.value) {
        val m = noteVM.item.value
        if (m != null) {
            SelectTagsDialog(tagsVM, tagsState, tagsMapState, data = m) {
                noteVM.showSelectTagsDialog.value = false
            }
        }
    }
    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = "",
                scrollBehavior = scrollBehavior,
                actions = {
                    if (noteVM.editMode) {
                        PIconButton(
                            icon = R.drawable.undo,
                            contentDescription = stringResource(id = R.string.undo),
                            enabled = mdEditorVM.textFieldState.undoState.canUndo,
                            tint = MaterialTheme.colorScheme.onSurface
                        ) {
                            mdEditorVM.textFieldState.undoState.undo()
                        }
                        PIconButton(
                            icon = R.drawable.redo,
                            contentDescription = stringResource(id = R.string.redo),
                            enabled = mdEditorVM.textFieldState.undoState.canRedo,
                            tint = MaterialTheme.colorScheme.onSurface
                        ) {
                            mdEditorVM.textFieldState.undoState.redo()
                        }

                        PIconButton(
                            icon = R.drawable.wrap_text,
                            contentDescription = stringResource(R.string.wrap_content),
                            tint = MaterialTheme.colorScheme.onSurface,
                        ) {
                            mdEditorVM.toggleWrapContent(context)
                        }
                    } else if (id.isNotEmpty()) {
                        ActionButtonTags {
                            noteVM.showSelectTagsDialog.value = true
                        }
                    }
                    PIconButton(
                        icon = if (noteVM.editMode) R.drawable.markdown else R.drawable.square_pen,
                        contentDescription = stringResource(if (noteVM.editMode) R.string.view else R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        noteVM.editMode = !noteVM.editMode
                    }
                },
            )
        },
        modifier = Modifier
            .imePadding(),
        bottomBar = {
            AnimatedVisibility(
                visible = noteVM.editMode,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                MdEditorBottomAppBar(mdEditorVM)
            }
        },
        content = { paddingValues ->
            if (noteVM.editMode) {
                MdEditor(
                    modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding(), top = paddingValues.calculateTopPadding()),
                    mdEditorVM = mdEditorVM,
                    scrollState = editorScrollState,
                    focusRequester = focusRequester
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    state = mdListState
                ) {
                    item {
                        val tags = tagsState.filter { tagIds.contains(it.id) }
                        if (tags.isNotEmpty()) {
                            PCard {
                                FlowRow(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    tags.forEach { tag ->
                                        Text(
                                            text = AnnotatedString("#" + tag.name),
                                            modifier = Modifier
                                                .wrapContentHeight()
                                                .align(Alignment.Bottom),
                                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary),
                                        )
                                    }
                                }
                            }
                            VerticalSpace(dp = 16.dp)
                        }
                    }
                    item {
                        MarkdownText(
                            text = noteVM.content,
                            modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
                            previewerState = previewerState,
                        )
                    }
                    item {
                        VerticalSpace(dp = paddingValues.calculateBottomPadding())
                        BottomSpace()
                    }
                }
            }

        },
    )

    MediaPreviewer(state = previewerState)
}
