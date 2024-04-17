package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileMediaStoreHelper
import com.ismartcoding.plain.preference.EditorWrapContentPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TextFileViewModel : ViewModel() {
    val showLoading = mutableStateOf(true)
    val isEditorReady = mutableStateOf(false)
    val wrapContent = mutableStateOf(true)
    val showMoreActions = mutableStateOf(false)
    val file = mutableStateOf<DFile?>(null)
    val content = mutableStateOf("")

    suspend fun loadConfigAsync(context: Context) {
        wrapContent.value = EditorWrapContentPreference.getAsync(context)
    }

    fun loadFileAsync(context: Context, path: String, mediaStoreId: String) {
        if (mediaStoreId.isNotEmpty()) {
            file.value = FileMediaStoreHelper.getById(context, mediaStoreId)
        }
        content.value = File(path).readText()
    }

    fun toggleWrapContent(context: Context) {
        wrapContent.value = !wrapContent.value
        viewModelScope.launch(Dispatchers.IO) {
            EditorWrapContentPreference.putAsync(context, wrapContent.value)
        }
    }
}
