package com.ismartcoding.plain.ui.page.shares

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.enums.ButtonType
import com.ismartcoding.plain.features.share.ShareExpiry
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.edit_share_link
import com.ismartcoding.plain.i18n.name
import com.ismartcoding.plain.i18n.add_items
import com.ismartcoding.plain.i18n.confirm_to_delete
import com.ismartcoding.plain.i18n.delete
import com.ismartcoding.plain.i18n.file_text
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.folder_plus
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.i18n.shared_items
import com.ismartcoding.plain.i18n.x
import com.ismartcoding.plain.i18n.share_expired
import com.ismartcoding.plain.i18n.share_expires_on
import com.ismartcoding.plain.i18n.share_expiry
import com.ismartcoding.plain.i18n.share_expiry_never
import com.ismartcoding.plain.i18n.share_link
import com.ismartcoding.plain.i18n.share_link_desc
import com.ismartcoding.plain.i18n.share_name_placeholder
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.toBreakableUrl
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.shareText
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.CornerCopyCard
import com.ismartcoding.plain.ui.base.IconTextQrCodeButton
import com.ismartcoding.plain.ui.base.IconTextForwardButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.WebAddressBarQrDialog
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import com.ismartcoding.plain.chat.ShareSendHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.i18n.sent
import com.ismartcoding.plain.ui.page.files.label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * Page to edit an existing share link: rename it, change the expiry, and
 * access the link itself (copy, QR code, system share, forward to a chat).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditSharePage(
    navController: NavHostController,
    shareId: String,
) {
    val scope = rememberCoroutineScope()
    var share by remember { mutableStateOf<DShare?>(null) }
    var link by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf(ShareExpiry.NEVER) }
    var paths by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var showItemsPicker by remember { mutableStateOf(false) }
    val dirFlags = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(shareId) {
        val s = withIO { ShareManager.getShare(shareId) } ?: return@LaunchedEffect
        share = s
        name = s.name
        expiry = s.toExpiryOption()
        paths = s.data.map { it.realPath }
        link = ShareManager.buildLink(s)
    }

    LaunchedEffect(paths) {
        withIO {
            paths.forEach { p -> dirFlags.getOrPut(p) { statFile(p)?.isDir == true } }
        }
    }

    val current = share
    if (current == null) {
        // Unknown share id: nothing to edit.
        PScaffold(
            topBar = { PTopAppBar(navController = navController, title = stringResource(Res.string.edit_share_link)) },
        ) { }
        return
    }

    val save: () -> Unit = {
        scope.launch {
            isLoading = true
            val updated = withIO {
                ShareManager.updateShare(current.id, name, expiry.expiresAt(TimeHelper.now()), realPaths = paths)
            }
            isLoading = false
            if (updated != null) navController.popBackStack()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.edit_share_link),
                actions = {
                    PTextButton(text = stringResource(Res.string.save), onClick = save)
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TopSpace()
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
            Text(stringResource(Res.string.shared_items), style = MaterialTheme.typography.titleSmall)
            VerticalSpace(8.dp)
            paths.forEach { path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(if (dirFlags[path] == true) Res.drawable.folder else Res.drawable.file_text),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = path.getFilenameFromPath(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    PIconButton(
                        icon = Res.drawable.x,
                        contentDescription = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = paths.size > 1,
                    ) {
                        paths = paths - path
                    }
                }
            }
            VerticalSpace(8.dp)
            POutlinedButton(
                text = stringResource(Res.string.add_items),
                icon = painterResource(Res.drawable.folder_plus),
                onClick = { showItemsPicker = true },
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
            CornerCopyCard(label = stringResource(Res.string.share_link), text = link.toBreakableUrl())
            VerticalSpace(16.dp)
            ActionButtons {
                IconTextQrCodeButton { showQr = true }
                IconTextShareButton { shareText(link) }
                IconTextForwardButton { showForwardDialog = true }
            }
            VerticalSpace(16.dp)
            POutlinedButton(
                text = stringResource(Res.string.delete),
                type = ButtonType.DANGER,
                onClick = {
                    scope.launch {
                        confirmActionAsync(
                            Res.string.delete,
                            Res.string.confirm_to_delete,
                            callback = {
                                scope.launch {
                                    withIO { ShareManager.deleteShare(current.id) }
                                    navController.popBackStack()
                                }
                            },
                            danger = true
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            BottomSpace(paddingValues)
        }
    }

    if (showQr) {
        WebAddressBarQrDialog(url = link, onClose = { showQr = false })
    }

    if (showItemsPicker) {
        ShareItemsPickerSheet(
            initialSelected = paths.toSet(),
            onDismiss = { showItemsPicker = false },
            onConfirm = { picked ->
                paths = (paths + picked).distinct()
                showItemsPicker = false
            },
        )
    }

    if (showForwardDialog) {
        ForwardTargetDialog(
            onDismiss = { showForwardDialog = false },
            onTargetsSelected = { targets ->
                showForwardDialog = false
                scope.launch {
                    ShareSendHelper.sendContentAsync(
                        targets,
                        ShareSendHelper.buildShareContent(current, current.data.map { it.realPath }),
                    )
                    DialogHelper.showSuccess(Res.string.sent)
                }
            },
        )
    }
}

/** Human-readable expiry status of a share, e.g. "Expired" / "Expires …" / "Never". */
@Composable
fun DShare.expiryLabel(): String {
    val expiresAt = this.expiresAt
    return when {
        isExpired -> stringResource(Res.string.share_expired)
        expiresAt != null -> stringResource(Res.string.share_expires_on, expiresAt.formatDateTime())
        else -> stringResource(Res.string.share_expiry_never)
    }
}

/** Pick the expiry chip closest to the share's remaining lifetime (null = NEVER). */
private fun DShare.toExpiryOption(): ShareExpiry {
    val expiresAt = expiresAt ?: return ShareExpiry.NEVER
    val remainingHours = (expiresAt - TimeHelper.now()).inWholeHours
    return ShareExpiry.entries.filter { it != ShareExpiry.NEVER }
        .minByOrNull { abs(it.hours - remainingHours) }
        ?: ShareExpiry.NEVER
}
