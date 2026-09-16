package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.launchSafe
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getFileByMediaId
import com.ismartcoding.plain.platform.isContentUri
import com.ismartcoding.plain.preferences.EditorFontSizePreference
import com.ismartcoding.plain.preferences.EditorStatusBarPreference
import com.ismartcoding.plain.preferences.EditorWrapContentPreference
import com.ismartcoding.plain.ui.components.codeeditor.EditorController
import com.ismartcoding.plain.ui.components.codeeditor.EditorLoadState
import com.ismartcoding.plain.ui.helpers.DialogHelper

class TextFileViewModel : ViewModel() {
    val controller = EditorController(viewModelScope)
    val showMoreActions = mutableStateOf(false)
    val file = mutableStateOf<DFile?>(null)
    val isExternalFile = mutableStateOf(false)

    val isDataLoading: Boolean
        get() = controller.loadState.value !is EditorLoadState.Ready && controller.loadState.value !is EditorLoadState.Error

    fun loadConfigAsync() {
        viewModelScope.launchSafe {
            controller.wrapContent.value = EditorWrapContentPreference.getAsync()
            controller.fontSizeSp.value = EditorFontSizePreference.getAsync()
            controller.statusBarVisible.value = EditorStatusBarPreference.getAsync()
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
            EditorWrapContentPreference.putAsync(!controller.wrapContent.value)
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
