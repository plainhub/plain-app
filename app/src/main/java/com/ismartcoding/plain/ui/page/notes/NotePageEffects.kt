package com.ismartcoding.plain.ui.page.notes

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ismartcoding.plain.lib.extensions.cut
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.data.TagRelationStub
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.NoteHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.models.NoteViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
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
    notesVM: NotesViewModel, tagsVM: TagsViewModel, mdEditorVM: MdEditorViewModel,
    previewerState: MediaPreviewerState,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val insetsController = WindowCompat.getInsetsController(window, view)

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = DataType.NOTE
        noteVM.editMode.value = id.value.isEmpty()
        mdEditorVM.load(context)
        scope.launch(Dispatchers.IO) {
            if (id.value.isNotEmpty()) {
                val item = NoteHelper.getById(id.value)
                noteVM.item.value = item
                noteVM.content.value = item?.content ?: ""
                mdEditorVM.textFieldState.edit { append(noteVM.content.value); setSelection(0) }
            }
            snapshotFlow { mdEditorVM.textFieldState.text }.debounce(200).collectLatest { t ->
                val isNew = id.value.isEmpty()
                val text = t.toString()
                if (noteVM.content.value == text) return@collectLatest
                scope.launch(Dispatchers.IO) {
                    val newItem = NoteHelper.addOrUpdateAsync(id.value) {
                        title = text.cut(250).replace("\n", ""); content = text; noteVM.content.value = text
                    }
                    id.value = newItem.id
                    if (isNew && tagId.isNotEmpty()) {
                        TagHelper.addTagRelations(arrayListOf(TagRelationStub(id.value).toTagRelation(tagId, DataType.NOTE)))
                    }
                    if (isNew) tagsVM.loadAsync(setOf(id.value))
                    notesVM.updateItem(newItem)
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { insetsController.show(WindowInsetsCompat.Type.navigationBars()) } }
    BackHandler(previewerState.visible) { scope.launch { previewerState.close() } }

    LaunchedEffect(noteVM.editMode.value) {
        if (noteVM.editMode.value) keyboardController?.show()
        else { keyboardController?.hide(); focusManager.clearFocus() }
    }

    SideEffect {
        if (noteVM.editMode.value && context.isGestureInteractionMode()) insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        else insetsController.show(WindowInsetsCompat.Type.navigationBars())
    }
}
