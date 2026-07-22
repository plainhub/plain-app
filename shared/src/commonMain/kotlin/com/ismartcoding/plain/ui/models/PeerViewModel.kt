package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference

class PeerViewModel : ViewModel() {
    fun updateDiscoverable(discoverable: Boolean) {
        viewModelScope.launchSafe {
            NearbyDiscoverablePreference.putAsync(discoverable)
            TempData.nearbyDiscoverable = discoverable
        }
    }

    fun removePeer(peerId: String) {
        viewModelScope.launchSafe {
            PeerManager.deletePeer(peerId)
        }
    }

    fun load() {
        viewModelScope.launchSafe { PeerManager.load() }
    }
}
