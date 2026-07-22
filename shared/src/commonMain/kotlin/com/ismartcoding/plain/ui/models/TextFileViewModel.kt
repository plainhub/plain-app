package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.extensions.toJsValue
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.EditorWebViewHandle
import com.ismartcoding.plain.platform.getFileByMediaId
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.readTextFile
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TextFileViewModel : ViewModel() {
    val isDataLoading = mutableStateOf(true)
    val isEditorReady = mutableStateOf(false)
    val wrapContent = mutableStateOf(true)
    val readOnly = mutableStateOf(true)
    val showMoreActions = mutableStateOf(false)
    val file = mutableStateOf<DFile?>(null)
    val webView = mutableStateOf<EditorWebViewHandle?>(null)
    val content = mutableStateOf("")
    val oldContent = mutableStateOf<String?>(null)
    val isExternalFile = mutableStateOf(false)

    suspend fun loadConfigAsync() {
        wrapContent.value = EditorWrapContentPreference.getAsync()
    }

    suspend fun loadFileAsync(path: String, mediaId: String) {
        try {
            if (mediaId.isNotEmpty()) {
                file.value = getFileByMediaId(mediaId)
            }

            // Set external file flag
            isExternalFile.value = isContentUri(path)

            // Read the file contents (handles both content:// URIs and regular paths
            // via the platform abstraction).
            content.value = readTextFile(path)
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(e.toString())
            LogCat.e(e.toString())
        }
    }

    fun toggleWrapContent() {
        wrapContent.value = !wrapContent.value
        viewModelScope.launchSafe {
            EditorWrapContentPreference.putAsync(wrapContent.value)
        }
        webView.value?.evaluateJavascript("editor.session.setUseWrapMode(${wrapContent.value.toJsValue()})")
    }

    fun gotoTop() {
        webView.value?.evaluateJavascript("editor.gotoLine(1)")
    }

    fun gotoEnd() {
        webView.value?.evaluateJavascript("editor.gotoLine(editor.session.getLength())")
    }

    fun enterEditMode() {
        readOnly.value = false
        oldContent.value = content.value
        webView.value?.evaluateJavascript("editor.setReadOnly(false)")
    }

    fun exitEditMode() {
        readOnly.value = true
        if (oldContent.value != null) {
            content.value = oldContent.value!!
            oldContent.value = null
            val json = buildJsonObject { put("content", content.value) }
            webView.value?.evaluateJavascript("updateContent($json)")
        }
        webView.value?.evaluateJavascript("editor.setReadOnly(true)")
    }
}
