package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.ismartcoding.plain.db.DNote

@OptIn(SavedStateHandleSaveableApi::class)
class NoteViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    var editMode by savedStateHandle.saveable { mutableStateOf(false) }
    val item = mutableStateOf<DNote?>(null)
    var content by savedStateHandle.saveable { mutableStateOf("") }
    val showSelectTagsDialog = mutableStateOf(false)
}
