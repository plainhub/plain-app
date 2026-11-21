package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.ismartcoding.plain.features.chat.ChatDraftAttachment
import com.ismartcoding.plain.features.media.MediaType
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.platform.files.fileUriPath
import com.ismartcoding.plain.platform.media.applyForceVideoDecoder
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.base.PIcon
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.folders as ui_drawable_folders
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.markdown as ui_drawable_markdown
import com.ismartcoding.plain.ui.resources.send as ui_drawable_send
import kotlinx.coroutines.launch

@Composable
fun ChatInput(
    value: String,
    previewerState: MediaPreviewerState,
    hint: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onSend: () -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onShareFolder: () -> Unit = {},
    filesAllowed: Boolean = true,
    onRequestFilePermission: () -> Unit = {},
    attachments: Map<String, ChatDraftAttachment> = emptyMap(),
    markdown: Boolean = false,
    sending: Boolean = false,
    onRemoveAttachment: (String) -> Unit = {},
    onChangeFormat: () -> Unit = {},
) {
    var hasFocus by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (attachments.isNotEmpty()) {
            Column(Modifier.heightIn(max = 128.dp).verticalScroll(rememberScrollState())) {
                attachments.forEach { (uri, attachment) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DraftAttachmentPreview(uri, attachment, previewerState)
                        if (attachment.mediaType == null) {
                            Text(attachment.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        PTextButton(text = stringResource(Res.string.remove), enabled = !sending, onClick = { onRemoveAttachment(uri) })
                    }
                }
            }
        }
        OutlinedTextField(
            value = value,
            enabled = !sending,
            onValueChange = { onValueChange(it) },
            modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                )
                .onFocusChanged { focusState -> hasFocus = focusState.hasFocus }
                .fillMaxWidth()
                .heightIn(max = 200.dp),
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                Text(
                    hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        if (hasFocus || attachments.isNotEmpty() || markdown) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!filesAllowed) {
                    com.ismartcoding.plain.ui.base.PTextButton(text = stringResource(Res.string.request_permission), onClick = onRequestFilePermission)
                } else {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_image,
                        contentDescription = stringResource(Res.string.images),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        if (!sending) sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.IMAGE_VIDEO, multiple = true))
                    }
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_folder,
                        contentDescription = stringResource(Res.string.files),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        if (!sending) sendEvent(PickFileEvent(PickFileTag.SEND_MESSAGE, PickFileType.FILE, multiple = true))
                    }
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_folders,
                        contentDescription = stringResource(Res.string.share_folder),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        if (!sending) onShareFolder()
                    }
                }
                IconToggleButton(
                    checked = markdown,
                    enabled = !sending,
                    onCheckedChange = { onChangeFormat() },
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    PIcon(
                        icon = painterResource(UiRes.drawable.ui_drawable_markdown),
                        contentDescription = stringResource(Res.string.chat_markdown),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                PIconButton(
                    icon = UiRes.drawable.ui_drawable_send,
                    contentDescription = stringResource(Res.string.send_message),
                    tint = MaterialTheme.colorScheme.primary,
                ) {
                    if (!sending) onSend()
                }
            }
        } else {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DraftAttachmentPreview(uri: String, attachment: ChatDraftAttachment, previewerState: MediaPreviewerState) {
    val name = attachment.name
    val icon = remember(name) { fileIconData(name.getFilenameExtension()) }
    val mediaType = attachment.mediaType
    if (mediaType == null) {
        AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.extraSmall))
        return
    }
    val context = LocalPlatformContext.current
    val request = remember(context, uri, mediaType) {
        ImageRequest.Builder(context).data(uri).applyForceVideoDecoder(mediaType == MediaType.VIDEO).build()
    }
    val iconPainter = rememberAsyncImagePainter(icon)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    AsyncImage(
        model = request,
        contentDescription = name,
        placeholder = iconPainter,
        error = iconPainter,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.preview)) {
                if (previewerState.visible || previewerState.animating) return@clickable
                keyboard?.hide()
                scope.launch {
                    val path = if (uri.startsWith("file://")) fileUriPath(uri) else uri
                    MediaPreviewData.items = listOf(PreviewItem(id = uri, path = path, pickedMediaType = mediaType))
                    previewerState.open()
                }
            },
    )
}
