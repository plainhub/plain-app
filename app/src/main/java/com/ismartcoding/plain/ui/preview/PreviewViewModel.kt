package com.ismartcoding.plain.ui.preview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PreviewViewModel : ViewModel() {
    val viewerUserInputEnabled = MutableLiveData<Boolean>()

    fun setViewerUserInputEnabled(enable: Boolean) {
        if (viewerUserInputEnabled.value != enable) viewerUserInputEnabled.value = enable
    }
}
