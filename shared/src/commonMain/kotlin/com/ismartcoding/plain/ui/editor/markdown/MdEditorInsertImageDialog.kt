package com.ismartcoding.plain.ui.editor.markdown

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.platform.media.PickImageEffect
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.ui.permissions.RemotePermissionEffects
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.ui.web.accessFeatureOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MdEditorInsertImageDialog(
    mdEditorVM: MdEditorViewModel,
) {
    val source = LocalContentSource.current
    val scope = rememberCoroutineScope()
    val permission = Permission.WRITE_EXTERNAL_STORAGE
    val canUpload = remember(source) { mutableStateOf(when (source) {
        ContentSource.Local -> true
        is ContentSource.Remote -> permission in source.client.currentInfo.permissions
    }) }
    if (source is ContentSource.Remote) RemotePermissionEffects(source, permission, canUpload)
    val imageUrlState = remember { mutableStateOf("") }
    var imageUrl by imageUrlState
    var description by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }

    PickImageEffect(imageUrl = imageUrlState)

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = {
            mdEditorVM.showInsertImage.value = false
        },
        confirmButton = {
            PFilledButton(
                text = stringResource(Res.string.confirm),
                buttonSize = ButtonSize.MEDIUM,
                onClick = {
                    var html = "<img src=\"${imageUrl}\""
                    if (width.isNotEmpty()) {
                        html += " width=\"${width}\""
                    }
                    if (description.isNotEmpty()) {
                        html += " alt=\"${description}\""
                    }
                    mdEditorVM.insertText("$html />")
                    mdEditorVM.showInsertImage.value = false
                },
            )
        },
        dismissButton = {
            PTextButton(text = stringResource(Res.string.cancel), onClick = { mdEditorVM.showInsertImage.value = false })
        },
        title = {
            Text(
                text = stringResource(Res.string.insert_image),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = imageUrl,
                        onValueChange = {
                            imageUrl = it
                        },
                        label = {
                            Text(text = stringResource(Res.string.image_url))
                        }
                    )
                    PFilledButton(
                        text = stringResource(if (canUpload.value) Res.string.browse else Res.string.request_permission),
                        buttonSize = ButtonSize.MEDIUM,
                        onClick = {
                            if (source is ContentSource.Remote && !canUpload.value) {
                                scope.launchSafe { source.api.execute(Mutation.openWebSettings(permission.accessFeatureOf())) }
                            } else sendEvent(PickFileEvent(PickFileTag.EDITOR, PickFileType.IMAGE, multiple = false))
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }
                VerticalSpace(dp = 8.dp)
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = {
                        Text(text = stringResource(Res.string.image_description))
                    })
                VerticalSpace(dp = 8.dp)
                OutlinedTextField(value = width, onValueChange = { width = it }, label = {
                    Text(text = stringResource(Res.string.width))
                })
            }
        })
}
