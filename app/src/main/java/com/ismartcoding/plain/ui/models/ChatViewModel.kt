package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class VChat(val id: String, val name: String, val createdAt: Instant, val type: String, var value: Any? = null) {
    companion object {
        fun from(data: DChat): VChat {
            return VChat(data.id, data.name, data.createdAt, data.content.type, data.content.value)
        }
    }
}

class ChatViewModel : ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VChat>())
    val itemsFlow: StateFlow<List<VChat>> get() = _itemsFlow

    fun fetch() {
        viewModelScope.launch(Dispatchers.IO) {
            _itemsFlow.value = AppDatabase.instance.chatDao().getAll().sortedByDescending { it.createdAt }.map { VChat.from(it) }.toMutableStateList()
        }
    }

    fun remove(id: String) {
        _itemsFlow.value.removeIf { it.id == id }
    }

    fun addAll(items: List<DChat>) {
        _itemsFlow.value.addAll(0, items.map { VChat.from(it) })
    }

    fun update(item: DChat) {
        val index = _itemsFlow.value.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            _itemsFlow.value[index] = _itemsFlow.value[index].copy(value = item.content.value)
        }
    }
}