package com.ismartcoding.plain.ui.page.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.models.ShareFile
import com.ismartcoding.plain.ui.models.ShareStage
import com.ismartcoding.plain.ui.models.ShareViewModel
import com.ismartcoding.plain.ui.page.chat.components.ChatTargetPicker
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    vm: ShareViewModel,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onOpen: () -> Unit,
    onSaveToFiles: () -> Unit,
    onSaveAsNote: () -> Unit,
    onOpenAsText: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    PModalBottomSheet(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = { if (!vm.sending) onDismiss() },
        sheetState = sheetState,
    ) {
        PBottomSheetTopAppBar(
            title = stringResource(if (vm.stage == ShareStage.ACTIONS) Res.string.share else Res.string.send_to_chat),
            navigationIcon = if (vm.stage == ShareStage.TARGETS) {
                {
                    PIconButton(
                        icon = Res.drawable.arrow_left,
                        contentDescription = stringResource(Res.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        enabled = !vm.sending,
                    ) { vm.stage = ShareStage.ACTIONS }
                }
            } else {
                null
            },
        )
        if (vm.stage == ShareStage.ACTIONS) {
            ActionsStage(vm, onOpen, onSaveToFiles, onSaveAsNote, onOpenAsText)
        } else {
            ChatTargetPicker(
                selectedIds = vm.selectedIds.toList(),
                onToggle = vm::toggle,
                onConfirm = onSend,
                enabled = !vm.sending,
            )
        }
        BottomSpace()
    }
}

@Composable
private fun ActionsStage(
    vm: ShareViewModel,
    onOpen: () -> Unit,
    onSaveToFiles: () -> Unit,
    onSaveAsNote: () -> Unit,
    onOpenAsText: () -> Unit,
) {
    val infos = vm.fileInfos
    val contentReady = if (vm.hasFiles) !infos.isNullOrEmpty() else !vm.text.isNullOrBlank()
    val openable = vm.hasFiles && infos?.size == 1 && isOpenable(infos.first())

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (vm.hasFiles) {
            FilePreview(vm, infos)
        } else {
            Text(
                text = vm.text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            )
        }

        PFilledButton(
            text = stringResource(Res.string.send_to_chat),
            onClick = { vm.stage = ShareStage.TARGETS },
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(Res.drawable.send),
            enabled = contentReady && !vm.sending,
        )
        if (openable) {
            POutlinedButton(
                text = stringResource(Res.string.open),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(Res.drawable.eye),
                enabled = !vm.sending,
            )
        }
        if (vm.hasFiles) {
            PTextButton(
                text = stringResource(Res.string.save_to_files),
                onClick = onSaveToFiles,
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(Res.drawable.download),
                enabled = contentReady && !vm.sending,
            )
        } else if (!vm.text.isSingleUrl()) {
            PTextButton(
                text = stringResource(Res.string.save_as_note),
                onClick = onSaveAsNote,
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(Res.drawable.notebook_pen),
            )
            PTextButton(
                text = stringResource(Res.string.open_as_text),
                onClick = onOpenAsText,
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(Res.drawable.file_text),
            )
        }
    }
}

@Composable
private fun FilePreview(vm: ShareViewModel, infos: List<ShareFile>?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            infos == null -> {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.share),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            infos.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.cannot_get_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> {
                Thumbnail(infos.first())
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = infos.first().name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (infos.size == 1) {
                        Text(
                            text = "${infos.first().mimeType} · ${infos.first().size.formatBytes()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.files_with_size, infos.size, infos.sumOf { it.size }.formatBytes()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(file: ShareFile) {
    if (file.mimeType.startsWith("image/")) {
        AsyncImage(
            model = file.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        AsyncImage(
            model = getFileIconPath(file.name.getFilenameExtension()),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
        )
    }
}

private fun isOpenable(file: ShareFile): Boolean =
    file.mimeType.startsWith("image/") ||
        file.mimeType.startsWith("text/") ||
        file.mimeType == "application/pdf" ||
        file.mimeType.startsWith("audio/") ||
        file.mimeType.startsWith("video/")

private fun String?.isSingleUrl(): Boolean {
    val t = this?.trim() ?: return false
    return !t.contains('\n') && !t.contains(' ') && (t.startsWith("http://") || t.startsWith("https://"))
}
