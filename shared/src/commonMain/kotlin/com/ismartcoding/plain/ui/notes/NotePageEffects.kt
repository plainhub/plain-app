package com.ismartcoding.plain.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import com.ismartcoding.plain.lib.extensions.getMarkdownTitle
import com.ismartcoding.plain.ui.platform.system.isGestureInteractionMode
import com.ismartcoding.plain.ui.platform.system.hideNavigationBar
import com.ismartcoding.plain.ui.platform.system.showNavigationBar
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.editor.markdown.MdEditorViewModel
import com.ismartcoding.plain.ui.notes.NoteViewModel
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.runtime.MutableState

@OptIn(FlowPreview::class)
@Composable
internal fun NotePageEffects(
    id: MutableState<String>, tagId: String, noteVM: NoteViewModel,
    tagsVM: TagsViewModel, mdEditorVM: MdEditorViewModel,
    previewerState: MediaPreviewerState,
    notesVM: NotesViewModel = NotesViewModel.local,
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = DataType.NOTE
        noteVM.editMode.value = true
        scope.launch(Dispatchers.Default) {
            if (id.value.isNotEmpty()) {
                val item = notesVM.api.getById(id.value)
                noteVM.item.value = item
                noteVM.content.value = item?.content ?: ""
            }
            mdEditorVM.loadText(noteVM.content.value)
            snapshotFlow { mdEditorVM.text() }.debounce(200).collectLatest { t ->
                val isNew = id.value.isEmpty()
                if (noteVM.content.value == t) return@collectLatest
                scope.launch(Dispatchers.Default) {
                    noteVM.content.value = t
                    val newItem = notesVM.api.save(id = id.value, title = "", content = t)
                    id.value = newItem.id
                    if (isNew && tagId.isNotEmpty()) {
                        tagsVM.api.edit(DataType.NOTE, listOf(id.value), listOf(tagId), emptyList())
                    }
                    if (isNew) {
                        tagsVM.loadAsync(setOf(id.value))
                    }
                    notesVM.updateItem(newItem)
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { showNavigationBar() } }
    PBackHandler(previewerState.visible) { scope.launch { previewerState.close() } }

    LaunchedEffect(noteVM.editMode.value) {
        if (noteVM.editMode.value) keyboardController?.show()
        else { keyboardController?.hide(); focusManager.clearFocus() }
    }

    SideEffect {
        if (noteVM.editMode.value && isGestureInteractionMode()) hideNavigationBar()
        else showNavigationBar()
    }
}
