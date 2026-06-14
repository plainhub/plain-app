package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.chat.ChannelManager
import com.ismartcoding.plain.ui.base.runHandling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun ChannelViewModel.addChannelMember(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.addMember(channelId, peerId) }
    }
}

internal fun ChannelViewModel.resendInvite(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.resendInvite(channelId, peerId) }
    }
}

internal fun ChannelViewModel.removeChannelMember(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.removeMember(channelId, peerId) }
    }
}

internal fun ChannelViewModel.leaveChannel(context: Context, channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.leaveChannel(channelId) }
    }
}

internal fun ChannelViewModel.acceptChannelInvite(channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.acceptInvite(channelId) }
    }
}

internal fun ChannelViewModel.declineChannelInvite(context: Context, channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        runHandling { ChannelManager.declineInvite(context, channelId) }
    }
}
