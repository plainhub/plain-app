package com.ismartcoding.plain.ui.page.files

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.share.ShareCrypto
import com.ismartcoding.plain.features.share.ShareExpiry
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cancel
import com.ismartcoding.plain.i18n.close
import com.ismartcoding.plain.i18n.create
import com.ismartcoding.plain.i18n.create_share_link
import com.ismartcoding.plain.i18n.name
import com.ismartcoding.plain.i18n.share_created
import com.ismartcoding.plain.i18n.share_expiry
import com.ismartcoding.plain.i18n.share_expiry_1d
import com.ismartcoding.plain.i18n.share_expiry_1h
import com.ismartcoding.plain.i18n.share_expiry_30d
import com.ismartcoding.plain.i18n.share_expiry_7d
import com.ismartcoding.plain.i18n.share_expiry_never
import com.ismartcoding.plain.i18n.share_link
import com.ismartcoding.plain.i18n.share_link_desc
import com.ismartcoding.plain.i18n.share_name_placeholder
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.toBreakableUrl
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.shareText
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.CornerCopyCard
import com.ismartcoding.plain.ui.base.IconTextQrCodeButton
import com.ismartcoding.plain.ui.base.IconTextForwardButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressBarQrDialog
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog to create a share link for the selected [paths]. First collects the
 * name / expiry options, then shows the generated link with copy, QR code and
 * system-share actions. Shares are always read-only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateShareDialog(
    paths: List<String>,
    onDismiss: () -> Unit,
    navController: NavHostController,
    chatVM: ChatViewModel,
    onCreated: (DShare) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val defaultName = remember(paths) { if (paths.size == 1) paths[0].getFilenameFromPath() else "" }
    var name by remember { mutableStateOf(defaultName) }
    var expiry by remember { mutableStateOf(ShareExpiry.NEVER) }
    var isLoading by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf<DShare?>(null) }
    var link by remember { mutableStateOf("") }
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
                onDismiss()
                navController.navigate(Routing.Chat(target.encodedToId))
            },
        )
        return
    }

    if (share != null) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.share_created), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.share_link_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalSpace(8.dp)
                    CornerCopyCard(label = stringResource(Res.string.share_link), text = link.toBreakableUrl())
                    VerticalSpace(8.dp)
                    ActionButtons {
                        IconTextQrCodeButton { showQr = true }
                        IconTextShareButton { shareText(link) }
                        IconTextForwardButton { showForwardDialog = true }
                    }
                }
            },
            confirmButton = {
                PFilledButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = onDismiss)
            },
        )
        return
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_share_link), style = MaterialTheme.typography.titleLarge) },
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
                    label = stringResource(Res.string.name),
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
            }
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.create),
                buttonSize = ButtonSize.MEDIUM,
                isLoading = isLoading,
                onClick = {
                    scope.launch {
                        isLoading = true
                        val created = withIO {
                            val s = ShareManager.createShare(
                                name = name.ifBlank { defaultName },
                                realPaths = paths,
                                urlToken = ShareCrypto.newUrlToken(),
                                readOnly = true,
                                expiresAt = expiry.expiresAt(TimeHelper.now()),
                            )
                            link = ShareManager.buildLink(s)
                            s
                        }
                        isLoading = false
                        share = created
                        onCreated(created)
                    }
                },
            )
        },
        dismissButton = {
            PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss)
        },
    )
}

internal val ShareExpiry.label: StringResource
    get() = when (this) {
        ShareExpiry.NEVER -> Res.string.share_expiry_never
        ShareExpiry.HOUR_1 -> Res.string.share_expiry_1h
        ShareExpiry.DAY_1 -> Res.string.share_expiry_1d
        ShareExpiry.DAY_7 -> Res.string.share_expiry_7d
        ShareExpiry.DAY_30 -> Res.string.share_expiry_30d
    }
