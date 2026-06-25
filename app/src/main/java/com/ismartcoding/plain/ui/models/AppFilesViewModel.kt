package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.ui.page.appfiles.AppFileDisplayNameHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AppFilesViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow<List<VAppFile>>(emptyList())
    val itemsFlow: StateFlow<List<VAppFile>> = _itemsFlow

    val showLoading = mutableStateOf(true)
    val offset = mutableIntStateOf(0)
    val limit = mutableIntStateOf(50)
    val noMore = mutableStateOf(false)
    val total = mutableIntStateOf(0)

    private suspend fun fetchPage(pageOffset: Int): List<VAppFile> = withIO {
        val appFileDao = AppDatabase.instance.appFileDao()
        val chatDao = AppDatabase.instance.chatDao()
        val files = appFileDao.getPage(limit.intValue, pageOffset)
        val nameMap = AppFileDisplayNameHelper.buildNameMap(chatDao.getAll())
        files.map { file ->
            VAppFile(
                appFile = file,
                fileName = AppFileDisplayNameHelper.resolveDisplayName(file, nameMap),
            )
        }
    }

    suspend fun moreAsync() = withIO {
        offset.intValue += limit.intValue
        val items = fetchPage(offset.intValue)
        _itemsFlow.update { it + items }
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }

    suspend fun loadAsync() = withIO {
        offset.intValue = 0
        val appFileDao = AppDatabase.instance.appFileDao()
        total.intValue = appFileDao.count()
        val items = fetchPage(0)
        _itemsFlow.value = items
        noMore.value = items.size < limit.intValue
        showLoading.value = false
    }
}
