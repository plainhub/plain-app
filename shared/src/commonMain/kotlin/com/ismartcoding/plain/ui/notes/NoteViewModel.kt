package com.ismartcoding.plain.ui.notes

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.models.Note

class NoteViewModel : ViewModel() {
    internal val editorInitialized = mutableStateOf(false)
    var editMode = mutableStateOf(false)
    val item = mutableStateOf<Note?>(null)
    var content = mutableStateOf("")
    val showSelectTagsDialog = mutableStateOf(false)
}
