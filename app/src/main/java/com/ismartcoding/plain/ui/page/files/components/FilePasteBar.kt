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
                                val srcFile = File(it.id)
                                val dstDir = File(filesVM.selectedPath)
                                val srcCanonical = srcFile.canonicalFile
                                val dstCanonical = dstDir.canonicalFile

                                // Prevent moving a directory into itself or its descendants
                                if (srcCanonical.isDirectory &&
                                    (dstCanonical.path == srcCanonical.path ||
                                            dstCanonical.path.startsWith(srcCanonical.path + "/"))
                                ) {
                                    DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.cannot_move_folder_into_itself))
                                    return@forEach
                                }

                                val dstFile = File(dstCanonical, srcCanonical.name)
                                try {
                                    if (!dstFile.exists()) {
                                        srcCanonical.toPath().moveTo(dstFile.toPath(), true)
                                    } else {
                                        srcCanonical.toPath().moveTo(Path(dstFile.newPath()), true)
                                    }
                                } catch (e: Exception) {
                                    // Fallback: copy then delete (handles cross-filesystem moves)
                                    try {
                                        val target = if (!dstFile.exists()) dstFile else File(dstFile.newPath())
                                        if (srcCanonical.isDirectory) {
                                            srcCanonical.copyRecursively(target, true)
                                            srcCanonical.deleteRecursively()
                                        } else {
                                            srcCanonical.copyTo(target, true)
                                            srcCanonical.delete()
                                        }
                                    } catch (ex: Exception) {
                                        DialogHelper.showErrorMessage(ex.message ?: LocaleHelper.getString(R.string.unknown_error))
                                    }
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
                                val srcFile = File(it.id)
                                val dstDir = File(filesVM.selectedPath)
                                val srcCanonical = srcFile.canonicalFile
                                val dstCanonical = dstDir.canonicalFile

                                // Prevent copying a directory into itself or its descendants
                                if (srcCanonical.isDirectory &&
                                    (dstCanonical.path == srcCanonical.path ||
                                            dstCanonical.path.startsWith(srcCanonical.path + "/"))
                                ) {
                                    DialogHelper.showErrorMessage(LocaleHelper.getString(R.string.cannot_copy_folder_into_itself))
                                    return@forEach
                                }

                                val dstFile = File(dstCanonical, srcCanonical.name)
                                try {
                                    if (!dstFile.exists()) {
                                        srcCanonical.copyRecursively(dstFile, true)
                                    } else {
                                        srcCanonical.copyRecursively(File(dstFile.newPath()), true)
                                    }
                                } catch (e: Exception) {
                                    DialogHelper.showErrorMessage(e.message ?: LocaleHelper.getString(R.string.unknown_error))
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