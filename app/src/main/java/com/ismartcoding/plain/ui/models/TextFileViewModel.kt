package com.ismartcoding.plain.ui.models

import android.content.Context
import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.extensions.toJsValue
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileMediaStoreHelper
import com.ismartcoding.plain.preference.EditorWrapContentPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TextFileViewModel : ViewModel() {
    val isDataLoading = mutableStateOf(true)
    val isEditorReady = mutableStateOf(false)
    val wrapContent = mutableStateOf(true)
    val readOnly = mutableStateOf(true)
    val showMoreActions = mutableStateOf(false)
    val file = mutableStateOf<DFile?>(null)
    val webView = mutableStateOf<WebView?>(null)
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
        webView.value?.evaluateJavascript("editor.session.setUseWrapMode(${wrapContent.value.toJsValue()})") {}
    }

    fun gotoTop() {
        webView.value?.evaluateJavascript("editor.gotoLine(1)") {}
    }

    fun gotoEnd() {
        webView.value?.evaluateJavascript("editor.gotoLine(editor.session.getLength())") {}
    }

    fun enterEditMode() {
        readOnly.value = false
        webView.value?.evaluateJavascript("editor.setReadOnly(false)") {}
    }

    fun exitEditMode() {
        readOnly.value = true
        webView.value?.evaluateJavascript("editor.setReadOnly(true)") {}
    }
}
