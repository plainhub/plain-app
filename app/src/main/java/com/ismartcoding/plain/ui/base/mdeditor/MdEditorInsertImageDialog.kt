package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.models.MdEditorViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MdEditorInsertImageDialog(
    mdEditorVM: MdEditorViewModel,
) {
    val context = LocalContext.current
    val imageUrlState = remember { mutableStateOf("") }
    var imageUrl by imageUrlState
    var description by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }

    PickImageEffect(context = context, imageUrl = imageUrlState)

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            mdEditorVM.showInsertImage = false
        },
        confirmButton = {
            Button(
                onClick = {
                    var html = "<img src=\"${imageUrl}\""
                    if (width.isNotEmpty()) {
                        html += " width=\"${width}\""
                    }
                    if (description.isNotEmpty()) {
                        html += " alt=\"${description}\""
                    }
                    mdEditorVM.textFieldState.edit {
                        add("$html />")
                    }
                    mdEditorVM.showInsertImage = false
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                mdEditorVM.showInsertImage = false
            }) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.insert_image),
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
                            Text(text = stringResource(id = R.string.image_url))
                        }
                    )
                    Button(
                        onClick = {
                            sendEvent(PickFileEvent(PickFileTag.EDITOR, PickFileType.IMAGE, multiple = false))
                        }, modifier = Modifier
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            stringResource(id = R.string.browse),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                VerticalSpace(dp = 8.dp)
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = {
                        Text(text = stringResource(id = R.string.image_description))
                    })
                VerticalSpace(dp = 8.dp)
                OutlinedTextField(value = width, onValueChange = { width = it }, label = {
                    Text(text = stringResource(id = R.string.width))
                })
            }
        })
}