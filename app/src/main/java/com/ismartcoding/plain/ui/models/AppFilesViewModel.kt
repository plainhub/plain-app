package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppFilesViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VAppFile>>(emptyList())
    val itemsFlow = _itemsFlow.asStateFlow()

    val isLoading = mutableStateOf(false)

    suspend fun loadAsync() {
        isLoading.value = true
        val appFileDao = AppDatabase.instance.appFileDao()
        val chatDao = AppDatabase.instance.chatDao()
        val files = appFileDao.getAll()
        val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
        _itemsFlow.value = files.map { file ->
            VAppFile(
                appFile = file,
                fileName = AppFileDisplayNameHelper.resolveDisplayName(file, nameMap),
            )
        }
        isLoading.value = false
    }
}
