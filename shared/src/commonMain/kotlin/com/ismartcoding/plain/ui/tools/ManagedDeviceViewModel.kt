package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.models.BusinessException
import com.ismartcoding.plain.models.ErrorCode
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal val LocalManagedDeviceViewModel = staticCompositionLocalOf<ManagedDeviceViewModel?> { null }

internal data class ManagedDeviceState(
    val client: ManagedDeviceClient? = null,
    val loading: Boolean = true,
    val error: Throwable? = null,
)

internal class ManagedDeviceViewModel(
    val peerId: String,
    val publicKey: String,
    private val readPeers: suspend () -> List<Peer> = { GraphqlApi.execute(Query.peers()) },
    private val httpForPeer: (Peer) -> PeerHttpClient = { PeerHttpClient(it) },
) : ViewModel() {
    private val _state = MutableStateFlow(ManagedDeviceState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    private suspend fun selectedPeer(): Peer {
        val peer = readPeers().find { it.id == peerId } ?: throw BusinessException(ErrorCode.PEER_REMOVED)
        if (!(peer.isPaired() && peer.token.isNotEmpty())) throw BusinessException(ErrorCode.NO_PERMISSION)
        if (!(peer.publicKey == publicKey)) throw BusinessException(ErrorCode.PEER_IDENTITY_CHANGED)
        return peer
    }

    suspend fun rename(name: String) {
        httpForPeer(selectedPeer()).graphql.execute(Mutation.updateDeviceName(name))
        refresh()
    }

    suspend fun refresh(): Boolean {
        try {
            val existing = _state.value.client
            val source = if (existing == null) {
                val peer = selectedPeer()
                ManagedDeviceClient.load(peer, httpForPeer(peer))
            } else {
                existing.info()
                existing
            }
            _state.value = ManagedDeviceState(client = source, loading = false)
            return true
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (error: Exception) {
            LogCat.e(error)
            _state.value = _state.value.copy(loading = false, error = error)
            return false
        }
    }
}
