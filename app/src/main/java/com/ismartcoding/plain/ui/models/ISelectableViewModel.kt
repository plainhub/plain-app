package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.data.IData
import kotlinx.coroutines.flow.StateFlow

interface ISelectableViewModel<T : IData> {
    var selectMode: MutableState<Boolean>
    val selectedIds: MutableList<String>
    val itemsFlow: StateFlow<List<T>>
}

fun <T : IData> ISelectableViewModel<T>.toggleSelectMode() {
    selectMode.value = !selectMode.value
    selectedIds.clear()
}

fun <T : IData> ISelectableViewModel<T>.exitSelectMode() {
    selectMode.value = false
    selectedIds.clear()
}

fun <T : IData> ISelectableViewModel<T>.enterSelectMode() {
    selectMode.value = true
}

fun <T : IData> ISelectableViewModel<T>.select(id: String) {
    if (selectedIds.contains(id)) {
        selectedIds.remove(id)
    } else {
        selectedIds.add(id)
    }
}

fun <T : IData> ISelectableViewModel<T>.isAllSelected(): Boolean {
    return selectedIds.size == itemsFlow.value.size
}

fun <T : IData> ISelectableViewModel<T>.toggleSelectAll() {
    if (isAllSelected()) {
        selectedIds.clear()
    } else {
        selectedIds.clear()
        selectedIds.addAll(itemsFlow.value.map { it.id })
    }
}