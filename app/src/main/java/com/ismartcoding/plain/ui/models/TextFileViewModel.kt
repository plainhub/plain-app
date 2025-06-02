package com.ismartcoding.plain.ui.models

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.extensions.toJsValue
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.preference.EditorWrapContentPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    val oldContent = mutableStateOf<String?>(null)
    val isExternalFile = mutableStateOf(false)

    suspend fun loadConfigAsync(context: Context) {
        wrapContent.value = EditorWrapContentPreference.getAsync(context)
    }

    fun loadFileAsync(context: Context, path: String, mediaId: String) {
        try {
            if (mediaId.isNotEmpty()) {
                file.value = FileMediaStoreHelper.getByIdAsync(context, mediaId)
            }
            
            // Set external file flag
            isExternalFile.value = path.startsWith("content://")
            
            // Handle content:// URIs and regular file paths
            content.value = if (path.startsWith("content://")) {
                // For content:// URIs, use ContentResolver
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                } ?: ""
            } else {
                // For regular file paths, use File.readText()
                File(path).readText()
            }
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(e.toString())
            LogCat.e(e.toString())
            e.printStackTrace()
        }
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
        oldContent.value = content.value
        webView.value?.evaluateJavascript("editor.setReadOnly(false)") {}
    }

    fun exitEditMode() {
        readOnly.value = true
        if (oldContent.value != null) {
            content.value = oldContent.value!!
            oldContent.value = null
            val json = JSONObject()
            json.put("content", content.value)
            webView.value?.evaluateJavascript("updateContent($json)") {}
        }
        webView.value?.evaluateJavascript("editor.setReadOnly(true)") {}
    }
}
