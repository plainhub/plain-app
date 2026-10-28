package com.ismartcoding.plain.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ManagedDeviceState(
    val info: ManagedDeviceInfo? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

internal class ManagedDeviceViewModel(val peerId: String, val publicKey: String) : ViewModel() {
    val client = viewModelScope.async {
        val peer = GraphqlApi.execute(Query.peers()).find { it.id == peerId } ?: error("peer_removed")
        check(peer.isPaired() && peer.token.isNotEmpty()) { "no_permission" }
        check(peer.publicKey == publicKey) { "peer_identity_changed" }
        ManagedDeviceClient(peer)
    }
    private val _state = MutableStateFlow(ManagedDeviceState())
    val state = _state.asStateFlow()

    suspend fun refresh() {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            _state.value = ManagedDeviceState(info = client.await().info(), loading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _state.value = _state.value.copy(loading = false, error = error.message ?: error.toString())
        }
    }
}
