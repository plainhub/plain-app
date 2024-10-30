package com.ismartcoding.plain.ui.page.files.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.newPath
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.moveTo

@Composable
fun FilePasteBar(
    filesVM: FilesViewModel,
    coroutineScope: CoroutineScope,
    onPasteComplete: () -> Unit
) {
    val context = LocalContext.current
    
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                filesVM.cutFiles.clear()
                filesVM.copyFiles.clear()
                filesVM.showPasteBar.value = false
            }) {
                Icon(painter = painterResource(R.drawable.x), contentDescription = "Cancel")
            }

            Text(
                text = if (filesVM.cutFiles.isNotEmpty())
                    LocaleHelper.getQuantityString(R.plurals.moving_items, filesVM.cutFiles.size)
                else
                    LocaleHelper.getQuantityString(R.plurals.copying_items, filesVM.copyFiles.size),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(onClick = {
                coroutineScope.launch {
                    if (filesVM.cutFiles.isNotEmpty()) {
                        DialogHelper.showLoading()
                        withContext(Dispatchers.IO) {
                            filesVM.cutFiles.forEach {
                                val dstFile = File(filesVM.path + "/" + it.id.getFilenameFromPath())
                                if (!dstFile.exists()) {
                                    Path(it.id).moveTo(dstFile.toPath(), true)
                                } else {
                                    Path(it.id).moveTo(Path(dstFile.newPath()), true)
                                }
                            }
                            filesVM.cutFiles.clear()
                        }
                        DialogHelper.hideLoading()
                        onPasteComplete()
                        filesVM.showPasteBar.value = false
                    } else if (filesVM.copyFiles.isNotEmpty()) {
                        DialogHelper.showLoading()
                        withContext(Dispatchers.IO) {
                            filesVM.copyFiles.forEach {
                                val dstFile = File(filesVM.path + "/" + it.id.getFilenameFromPath())
                                if (!dstFile.exists()) {
                                    File(it.id).copyRecursively(dstFile, true)
                                } else {
                                    File(it.id).copyRecursively(File(dstFile.newPath()), true)
                                }
                            }
                            filesVM.copyFiles.clear()
                        }
                        DialogHelper.hideLoading()
                        onPasteComplete()
                        filesVM.showPasteBar.value = false
                    }
                }
            }) {
                Text(stringResource(R.string.paste))
            }
        }
    }
} 