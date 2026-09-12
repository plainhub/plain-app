package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.queryPickedFileInfo
import kotlinx.coroutines.launch

enum class ShareStage { ACTIONS, TARGETS }

data class ShareFile(val uri: String, val name: String, val size: Long, val mimeType: String)

class ShareViewModel : ViewModel() {
    var text by mutableStateOf<String?>(null)
    var caption by mutableStateOf("")
    var hasFiles by mutableStateOf(false)
    var fileInfos by mutableStateOf<List<ShareFile>?>(null)
    var stage by mutableStateOf(ShareStage.ACTIONS)
    var sending by mutableStateOf(false)
    val selectedIds = mutableStateListOf<String>()

    fun load(sharedText: String?, uris: List<String>, caption: String = "") {
        text = sharedText
        this.caption = caption
        if (uris.isEmpty()) {
            hasFiles = false
            fileInfos = emptyList()
            return
        }
        hasFiles = true
        fileInfos = null
        viewModelScope.launch {
            fileInfos = withIO {
                uris.mapNotNull { uri ->
                    queryPickedFileInfo(uri)?.let { ShareFile(uri, it.displayName, it.size, it.mimeType) }
                }
            }
        }
    }

    fun reset() {
        text = null
        caption = ""
        hasFiles = false
        fileInfos = null
        stage = ShareStage.ACTIONS
        sending = false
        selectedIds.clear()
    }

    fun toggle(id: String) {
        if (!selectedIds.remove(id)) selectedIds.add(id)
    }
}
