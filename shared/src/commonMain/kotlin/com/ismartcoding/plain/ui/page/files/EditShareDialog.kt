package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.share.ShareExpiry
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.shareText
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.IconTextSmallButtonForward
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressBarQrDialog
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Dialog to edit an existing share link: rename it, change the expiry, and
 * access the link itself (copy, QR code, system share, forward to a chat).
 * Mirrors [CreateShareDialog].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditShareDialog(
    share: DShare,
    link: String,
    onDismiss: () -> Unit,
    navController: NavHostController,
    chatVM: ChatViewModel,
    onSaved: (DShare) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember(share.id) { mutableStateOf(share.name) }
    var expiry by remember(share.id) { mutableStateOf(share.toExpiryOption()) }
    var isLoading by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }

    if (showQr) {
        WebAddressBarQrDialog(url = link, onClose = { showQr = false })
        return
    }

    if (showForwardDialog) {
        ForwardTargetDialog(
            onDismiss = { showForwardDialog = false },
            onTargetSelected = { target ->
                chatVM.setPendingForwardText(link)
                showForwardDialog = false
                navController.navigate(Routing.Chat(target.encodedToId))
            },
        )
        return
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_share_link), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.share_link_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalSpace(8.dp)
                ClipboardTextField(
                    value = name,
                    placeholder = stringResource(Res.string.share_name_placeholder),
                    onValueChange = { name = it },
                )
                VerticalSpace(16.dp)
                Text(stringResource(Res.string.share_expiry), style = MaterialTheme.typography.titleSmall)
                VerticalSpace(8.dp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareExpiry.entries.forEach { e ->
                        PFilterChip(
                            selected = expiry == e,
                            onClick = { expiry = e },
                            label = { Text(stringResource(e.label)) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                VerticalSpace(8.dp)
                PCard {
                    PListItem(title = link, action = {
                        CopyIconButton(
                            text = link,
                            clipLabel = stringResource(Res.string.share_link),
                            copiedMessage = stringResource(Res.string.share_link_copied),
                        )
                    })
                }
                VerticalSpace(8.dp)
                BottomActionButtons {
                    PIconTextSmallButton(icon = Res.drawable.qr_code, text = stringResource(Res.string.qrcode)) { showQr = true }
                    PIconTextSmallButton(icon = Res.drawable.share_2, text = stringResource(Res.string.share)) { shareText(link) }
                    IconTextSmallButtonForward { showForwardDialog = true }
                }
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.save),
                buttonSize = ButtonSize.MEDIUM,
                isLoading = isLoading,
                onClick = {
                    scope.launch {
                        isLoading = true
                        val updated = withIO {
                            ShareManager.updateShare(share.id, name, expiry.expiresAt(TimeHelper.now()))
                        }
                        isLoading = false
                        if (updated != null) onSaved(updated)
                    }
                },
            )
        },
        dismissButton = {
            PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        },
    )
}

/** Pick the expiry chip closest to the share's remaining lifetime (null = NEVER). */
private fun DShare.toExpiryOption(): ShareExpiry {
    val expiresAt = expiresAt ?: return ShareExpiry.NEVER
    val remainingHours = (expiresAt - TimeHelper.now()).inWholeHours
    return ShareExpiry.entries.filter { it != ShareExpiry.NEVER }
        .minByOrNull { abs(it.hours - remainingHours) }
        ?: ShareExpiry.NEVER
}
