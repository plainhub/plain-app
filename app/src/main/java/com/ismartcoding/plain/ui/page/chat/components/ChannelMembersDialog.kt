package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer

@Composable
fun ChannelMembersDialog(
    channel: DChatChannel,
    pairedPeers: List<DPeer>,
    onAddMember: (peerId: String) -> Unit,
    onRemoveMember: (peerId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Track members locally so that Add/Remove actions update the UI immediately
    val currentMembers = remember { mutableStateListOf<ChannelMember>().apply { addAll(channel.members) } }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.channel_members),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            if (pairedPeers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.no_paired_peers_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(pairedPeers, key = { it.id }) { peer ->
                        val member = currentMembers.find { it.id == peer.id }
                        val isMember = member != null
                        val isPending = member?.isPending() == true
                        PeerMemberRow(
                            peer = peer,
                            isMember = isMember,
                            isPending = isPending,
                            onAdd = {
                                if (member == null) {
                                    currentMembers.add(ChannelMember(id = peer.id, status = ChannelMember.STATUS_PENDING))
                                }
                                onAddMember(peer.id)
                            },
                            onRemove = {
                                currentMembers.removeAll { it.id == peer.id }
                                onRemoveMember(peer.id)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.done))
            }
        },
        dismissButton = {},
    )
}
