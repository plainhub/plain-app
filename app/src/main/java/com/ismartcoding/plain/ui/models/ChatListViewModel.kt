package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.web.ChatApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatListViewModel : ViewModel() {
    val peers = mutableStateOf<List<DPeer>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun loadPeers(context: Context) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val peersList = withContext(Dispatchers.IO) {
                    AppDatabase.instance.peerDao().getAll().filter { it.status == "paired" }
                }
                peers.value = peersList
            } catch (e: Exception) {
                // Handle error if needed
                peers.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refreshPeers(context: Context) {
        loadPeers(context)
    }

    fun getPeerById(id: String): DPeer? {
        return peers.value.find { it.id == id }
    }

    fun removePeer(context: Context, peerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppDatabase.instance.peerDao().delete(peerId)
                ChatApiManager.loadKeyCacheAsync()
                loadPeers(context)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
} 