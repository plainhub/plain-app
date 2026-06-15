package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.models.PeerViewModel

@Composable
fun ForwardTargetDialog(
    peerVM: PeerViewModel,
    onDismiss: () -> Unit,
    onTargetSelected: (ChatTarget) -> Unit
) {
    val pairedPeers = peerVM.pairedPeers

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.forward),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(modifier = Modifier.defaultMinSize(minHeight = 100.dp)) {
                item {
                    PDialogListItem(
                        modifier = Modifier.clickable {
                            onTargetSelected(ChatTarget("local", ChatTargetType.LOCAL))
                            onDismiss()
                        },
                        title = stringResource(Res.string.local_chat),
                        subtitle = stringResource(Res.string.local_chat_desc),
                        showMore = true
                    )
                }

                if (pairedPeers.isNotEmpty()) {
                    items(pairedPeers, key = { it.id }) { peer ->
                        PDialogListItem(
                            modifier = Modifier.clickable {
                                onTargetSelected(ChatTarget(peer.id, ChatTargetType.PEER))
                                onDismiss()
                            },
                            title = peer.name,
                            subtitle = peer.ip,
                            showMore = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(Res.string.close))
            }
        }
    )
}

