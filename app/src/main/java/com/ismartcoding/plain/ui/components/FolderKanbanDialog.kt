package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.events.FolderKanbanSelectEvent
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preference.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.FolderOption
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderKanbanDialog(filesVM: FilesViewModel, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val options = remember { mutableStateListOf<FolderOption>() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val recentsText = stringResource(R.string.recents)
    val internalStorageText = stringResource(R.string.internal_storage)
    val sdcardText = stringResource(R.string.sdcard)
    val usbStorageText = stringResource(R.string.usb_storage)
    val fileTransferAssistantText = stringResource(R.string.app_data)

    LaunchedEffect(Unit) {
        val items = withIO {
            // Cache frequently used paths
            val internalStoragePath = FileSystemHelper.getInternalStoragePath()
            val externalFilesDirPath = FileSystemHelper.getExternalFilesDirPath(context)
            val sdCardPath = withContext(Dispatchers.IO) { FileSystemHelper.getSDCardPath(context) }
            val usbPaths = FileSystemHelper.getUsbDiskPaths()
            val favoriteFolders = FavoriteFoldersPreference.getValueAsync(context)

            // Helper function to find the longest matching prefix
            fun findLongestMatch(currentPath: String): String {
                val allPaths = mutableListOf<String>()
                
                // Add system paths
                allPaths.add(internalStoragePath)
                allPaths.add(externalFilesDirPath)
                if (sdCardPath.isNotEmpty()) {
                    allPaths.add(sdCardPath)
                }
                allPaths.addAll(usbPaths)
                
                // Add favorite folder paths
                favoriteFolders.forEach { favorite ->
                    if (java.io.File(favorite.fullPath).exists()) {
                        allPaths.add(favorite.fullPath)
                    }
                }
                
                // Find the longest matching prefix
                var longestMatch = ""
                allPaths.forEach { path ->
                    if (currentPath.startsWith(path) && path.length > longestMatch.length) {
                        longestMatch = path
                    }
                }
                
                return longestMatch
            }
            
            // Helper function to generate display title for favorite folders
            fun generateFavoriteDisplayTitle(favoriteFolder: DFavoriteFolder): String {
                val rootName = when {
                    favoriteFolder.rootPath == internalStoragePath -> FileSystemHelper.getInternalStorageName()
                    favoriteFolder.rootPath == externalFilesDirPath -> fileTransferAssistantText
                    favoriteFolder.rootPath == sdCardPath -> sdcardText
                    usbPaths.contains(favoriteFolder.rootPath) -> {
                        val index = usbPaths.indexOf(favoriteFolder.rootPath)
                        "$usbStorageText ${index + 1}"
                    }
                    else -> favoriteFolder.rootPath
                }

                // Calculate relative path from root to favorite folder
                val relativePath = if (favoriteFolder.fullPath.startsWith(favoriteFolder.rootPath)) {
                    favoriteFolder.fullPath.removePrefix(favoriteFolder.rootPath).removePrefix("/")
                } else {
                    java.io.File(favoriteFolder.fullPath).name
                }

                return if (relativePath.isNotEmpty()) {
                    "$rootName/$relativePath"
                } else {
                    rootName
                }
            }
            
            val longestMatchPath = findLongestMatch(filesVM.selectedPath)
            val menuItems = mutableListOf<FolderOption>()

            // Recents (special case)
            menuItems.add(
                FolderOption(
                    rootPath = "",
                    fullPath = "",
                    type = FilesType.RECENTS,
                    isChecked = filesVM.type == FilesType.RECENTS,
                    title = recentsText,
                )
            )

            // Internal Storage
            menuItems.add(
                FolderOption(
                    rootPath = internalStoragePath,
                    fullPath = internalStoragePath,
                    type = FilesType.INTERNAL_STORAGE,
                    isChecked = longestMatchPath == internalStoragePath,
                    title = internalStorageText,
                )
            )

            // SD Card (if available)
            if (sdCardPath.isNotEmpty()) {
                menuItems.add(
                    FolderOption(
                        rootPath = sdCardPath, 
                        fullPath = sdCardPath, 
                        type = FilesType.SDCARD, 
                        isChecked = longestMatchPath == sdCardPath,
                        title = sdcardText, 
                    )
                )
            }

            // USB Storage (if available)
            usbPaths.forEachIndexed { index, path ->
                menuItems.add(
                    FolderOption(
                        rootPath = path, 
                        fullPath = path, 
                        type = FilesType.USB_STORAGE, 
                        isChecked = longestMatchPath == path,
                        title = "$usbStorageText ${index + 1}", 
                    )
                )
            }

            // App Storage
            menuItems.add(
                FolderOption(
                    rootPath = externalFilesDirPath, 
                    fullPath = externalFilesDirPath, 
                    type = FilesType.APP, 
                    isChecked = longestMatchPath == externalFilesDirPath,
                    title = fileTransferAssistantText, 
                )
            )

            // Favorite folders
            favoriteFolders.forEach { favoriteFolder ->
                if (java.io.File(favoriteFolder.fullPath).exists()) {
                    val displayTitle = generateFavoriteDisplayTitle(favoriteFolder)

                    menuItems.add(
                        FolderOption(
                            rootPath = favoriteFolder.rootPath,
                            fullPath = favoriteFolder.fullPath,
                            type = FilesType.INTERNAL_STORAGE,
                            isChecked = longestMatchPath == favoriteFolder.fullPath,
                            title = displayTitle,
                            isFavoriteFolder = true
                        )
                    )
                }
            }

            menuItems
        }

        options.clear()
        options.addAll(items)
    }

    PModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = stringResource(R.string.folders),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            if (options.isNotEmpty()) {
                LazyColumn(
                    Modifier.fillMaxSize()
                ) {
                    item {
                        TopSpace()
                    }
                    items(options) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sendEvent(FolderKanbanSelectEvent(item))
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = item.isChecked,
                                onClick = {
                                    sendEvent(FolderKanbanSelectEvent(item))
                                    onDismiss()
                                }
                            )
                            HorizontalSpace(8.dp)
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.isFavoriteFolder) {
                                PIconButton(
                                    icon = R.drawable.delete_forever,
                                    tint = MaterialTheme.colorScheme.red,
                                    contentDescription = stringResource(R.string.delete),
                                    click = {
                                        DialogHelper.confirmToDelete {
                                            scope.launch(Dispatchers.IO) {
                                                FavoriteFoldersPreference.removeAsync(context, item.fullPath)
                                                options.remove(item)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item {
                        BottomSpace()
                    }
                }
            } else {
                NoDataColumn(loading = true)
            }
        }
    }
} 