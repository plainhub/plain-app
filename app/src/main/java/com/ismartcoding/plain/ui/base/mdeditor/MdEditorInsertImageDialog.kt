package com.ismartcoding.plain.ui.base.mdeditor

import android.os.Environment
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.lib.extensions.queryOpenableFileName
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MdEditorInsertImageDialog(
    mdEditorVM: MdEditorViewModel,
) {
    val context = LocalContext.current
    var imageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.EDITOR) {
                        return@collect
                    }
                    val uri = event.uris.first()
                    try {
                        val fileName = context.contentResolver.queryOpenableFileName(uri)
                        if (fileName.isNotEmpty()) {
                            val dir = Environment.DIRECTORY_PICTURES
                            val dst = context.getExternalFilesDir(dir)!!.path + "/$fileName"
                            val dstFile = File(dst)
                            val path =
                                if (dstFile.exists()) {
                                    dstFile.newPath()
                                } else {
                                    dst
                                }
                            FileHelper.copyFile(context, uri, path)
                            imageUrl = "app://$dir/${path.getFilenameFromPath()}"
                        }
                    } catch (ex: Exception) {
                        // the picked file could be deleted
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

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