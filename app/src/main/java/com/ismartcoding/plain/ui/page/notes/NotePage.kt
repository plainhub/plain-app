package com.ismartcoding.plain.ui.page.notes

import com.ismartcoding.plain.i18n.*
import android.annotation.SuppressLint
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.helpers.ShareHelper
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
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.NoteViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.tags.SelectTagsDialog
import com.ismartcoding.plain.ui.theme.PlainTheme

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NotePage(
    navController: NavHostController, initId: String, tagId: String,
    notesVM: NotesViewModel, tagsVM: TagsViewModel,
    noteVM: NoteViewModel = viewModel(), mdEditorVM: MdEditorViewModel = viewModel(),
) {
    val context = LocalContext.current
    val id = remember { mutableStateOf(initId) }
    val previewerState = rememberPreviewerState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val mdListState = rememberLazyListState()
    val editorScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val shouldRequestFocus = remember { mutableStateOf(true) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = { !noteVM.editMode })
    val tagIds = tagsMapState[id.value]?.map { it.tagId } ?: emptyList()

    NotePageEffects(id, tagId, noteVM, notesVM, tagsVM, mdEditorVM, previewerState)

    if (noteVM.showSelectTagsDialog.value) {
        val m = noteVM.item.value
        if (m != null) SelectTagsDialog(tagsVM, tagsState, tagsMapState, data = m) { noteVM.showSelectTagsDialog.value = false }
    }

    PScaffold(topBar = {
        PTopAppBar(navController = navController, title = "", scrollBehavior = scrollBehavior, actions = {
            if (noteVM.editMode) {
                PIconButton(icon = Res.drawable.undo, contentDescription = stringResource(Res.string.undo), enabled = mdEditorVM.textFieldState.undoState.canUndo,
                    tint = MaterialTheme.colorScheme.onSurface) { mdEditorVM.textFieldState.undoState.undo() }
                PIconButton(icon = Res.drawable.redo, contentDescription = stringResource(Res.string.redo), enabled = mdEditorVM.textFieldState.undoState.canRedo,
                    tint = MaterialTheme.colorScheme.onSurface) { mdEditorVM.textFieldState.undoState.redo() }
                PIconButton(icon = Res.drawable.wrap_text, contentDescription = stringResource(Res.string.wrap_content),
                    tint = MaterialTheme.colorScheme.onSurface) { mdEditorVM.toggleWrapContent(navController.context) }
            } else if (id.value.isNotEmpty()) {
                ActionButtonTags { noteVM.showSelectTagsDialog.value = true }
                PIconButton(
                    icon = Res.drawable.share_2,
                    contentDescription = stringResource(Res.string.share),
                    tint = MaterialTheme.colorScheme.onSurface,
                ) {
                    ShareHelper.shareText(context, noteVM.content)
                }
            }
            PIconButton(icon = if (noteVM.editMode) Res.drawable.markdown else Res.drawable.square_pen,
                contentDescription = stringResource(if (noteVM.editMode) Res.string.view else Res.string.edit),
                tint = MaterialTheme.colorScheme.onSurface) { noteVM.editMode = !noteVM.editMode }
        })
    }, modifier = Modifier.imePadding(), bottomBar = {
        AnimatedVisibility(visible = noteVM.editMode, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            MdEditorBottomAppBar(mdEditorVM)
        }
    }, content = { paddingValues ->
        if (noteVM.editMode) {
            MdEditor(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding(), top = paddingValues.calculateTopPadding()),
                mdEditorVM = mdEditorVM, scrollState = editorScrollState, focusRequester = focusRequester,
                shouldRequestFocus = shouldRequestFocus.value, onFocusRequested = { shouldRequestFocus.value = false })
        } else {
            LazyColumn(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).nestedScroll(scrollBehavior.nestedScrollConnection), state = mdListState) {
                item {
                    val tags = tagsState.filter { tagIds.contains(it.id) }
                    if (tags.isNotEmpty()) {
                        PCard {
                            FlowRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tags.forEach { tag ->
                                    Text(text = AnnotatedString("#" + tag.name), modifier = Modifier.wrapContentHeight().align(Alignment.Bottom),
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                        VerticalSpace(dp = 16.dp)
                    }
                }
                item { MarkdownText(text = noteVM.content, modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN), previewerState = previewerState) }
                item { BottomSpace(paddingValues) }
            }
        }
    })
    MediaPreviewer(state = previewerState)
}
