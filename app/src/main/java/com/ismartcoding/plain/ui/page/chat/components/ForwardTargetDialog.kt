package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.models.ChatListViewModel

sealed class ForwardTarget {
    data class Peer(val peer: DPeer) : ForwardTarget()
    object Local : ForwardTarget()
}

@Composable
fun ForwardTargetDialog(
    chatListVM: ChatListViewModel,
    onDismiss: () -> Unit,
    onTargetSelected: (ForwardTarget) -> Unit
) {
    val pairedPeers = chatListVM.pairedPeers
    
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.forward),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(modifier = Modifier.defaultMinSize(minHeight = 100.dp)) {
                item {
                    PDialogListItem(
                        modifier = Modifier.clickable {
                            onTargetSelected(ForwardTarget.Local)
                            onDismiss()
                        },
                        title = stringResource(id = R.string.local_chat),
                        desc = stringResource(id = R.string.local_chat_desc),
                        showMore = true
                    )
                }
                
                if (pairedPeers.isNotEmpty()) {
                    items(pairedPeers) { peer ->
                        PDialogListItem(
                            modifier = Modifier.clickable {
                                onTargetSelected(ForwardTarget.Peer(peer))
                                onDismiss()
                            },
                            title = peer.name,
                            desc = peer.ip,
                            showMore = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        }
    )
}

