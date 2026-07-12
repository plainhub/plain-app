package com.ismartcoding.plain.ui.models

import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference

class PeerViewModel : ViewModel() {
    fun updateDiscoverable(discoverable: Boolean) {
        launchSafe {
            NearbyDiscoverablePreference.putAsync(discoverable)
            TempData.nearbyDiscoverable = discoverable
        }
    }

    fun removePeer(peerId: String) {
        launchSafe {
            PeerManager.deletePeer(peerId)
        }
    }

    fun load() {
        launchSafe { PeerManager.load() }
    }
}
