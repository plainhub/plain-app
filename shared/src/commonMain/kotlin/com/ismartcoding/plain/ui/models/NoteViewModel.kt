package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.db.DNote

class NoteViewModel : ViewModel() {
    var editMode = mutableStateOf(false)
    val item = mutableStateOf<DNote?>(null)
    var content = mutableStateOf("")
    val showSelectTagsDialog = mutableStateOf(false)
}
