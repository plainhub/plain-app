package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.preferences.*

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getFileByMediaId
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.platform.openByteSource
import com.ismartcoding.plain.platform.writeByteChunksStreaming
import com.ismartcoding.plain.ui.components.codeeditor.EditorFileIO
import com.ismartcoding.plain.ui.components.codeeditor.EditorController
import com.ismartcoding.plain.ui.components.codeeditor.EditorLoadState
import com.ismartcoding.plain.ui.helpers.DialogHelper

class TextFileViewModel : ViewModel() {
    val controller = EditorController(viewModelScope, object : EditorFileIO {
        override fun nowMillis() = com.ismartcoding.plain.lib.TimeHelper.nowMillis()
        override fun openByteSource(path: String) = com.ismartcoding.plain.platform.openByteSource(path)
        override fun writeByteChunksStreaming(path: String, chunks: Iterator<ByteArray>) =
            com.ismartcoding.plain.platform.writeByteChunksStreaming(path, chunks)
    })
    val showMoreActions = mutableStateOf(false)
    val file = mutableStateOf<DFile?>(null)
    val isExternalFile = mutableStateOf(false)

    val isDataLoading: Boolean
        get() = controller.loadState.value !is EditorLoadState.Ready && controller.loadState.value !is EditorLoadState.Error

    fun loadConfigAsync() {
        viewModelScope.launchSafe {
            controller.wrapContent.value = UserPrefs.editorWrapContent.value
            controller.fontSizeSp.value = UserPrefs.editorFontSize.value
            controller.statusBarVisible.value = UserPrefs.editorStatusBar.value
        }
    }

    suspend fun loadFileAsync(path: String, mediaId: String, gotoEnd: Boolean) {
        try {
            if (mediaId.isNotEmpty()) {
                file.value = getFileByMediaId(mediaId)
            }
            isExternalFile.value = isContentUri(path)
            controller.open(path, gotoEnd)
        } catch (e: Exception) {
            DialogHelper.showErrorDialog(e.toString())
            LogCat.e(e.toString())
        }
    }

    fun toggleWrapContent() {
        viewModelScope.launchSafe {
            UserPrefs.editorWrapContent.value = !controller.wrapContent.value
        }
        controller.toggleWrap()
    }

    fun gotoTop() = controller.gotoTop()

    fun gotoEnd() = controller.gotoEnd()

    fun enterEditMode() = controller.enterEditMode()

    fun exitEditMode(discard: Boolean) = controller.exitEditMode(discard)

    override fun onCleared() {
        controller.close()
        super.onCleared()
    }
}
