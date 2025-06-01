package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.models.DrawerMenuItemClickedEvent
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.MenuItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FolderKanbanDialog(filesVM: FilesViewModel, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val options = remember { mutableStateListOf<RadioDialogOption>() }
    
    val recentsText = stringResource(R.string.recents)
    val sdcardText = stringResource(R.string.sdcard)
    val usbStorageText = stringResource(R.string.usb_storage)
    val fileTransferAssistantText = stringResource(R.string.app_data)
    
    LaunchedEffect(Unit) {
        val items = withIO {
            val menuItems = mutableListOf<MenuItemModel>()
            
            menuItems.add(
                MenuItemModel("").apply {
                    isChecked = filesVM.type == FilesType.RECENTS
                    title = recentsText
                    iconId = R.drawable.history
                }
            )
            
            menuItems.add(
                MenuItemModel(FileSystemHelper.getInternalStoragePath()).apply {
                    isChecked = filesVM.type == FilesType.INTERNAL_STORAGE
                    title = FileSystemHelper.getInternalStorageName()
                    iconId = R.drawable.hard_drive
                }
            )
            
            // SD Card (if available)
            val sdCardPath = withContext(Dispatchers.IO) { FileSystemHelper.getSDCardPath(context) }
            if (sdCardPath.isNotEmpty()) {
                menuItems.add(
                    MenuItemModel(sdCardPath).apply {
                        isChecked = filesVM.type == FilesType.SDCARD
                        title = sdcardText
                        iconId = R.drawable.sd_card
                    }
                )
            }
            
            // USB Storage (if available)
            val usbPaths = FileSystemHelper.getUsbDiskPaths()
            if (usbPaths.isNotEmpty()) {
                usbPaths.forEachIndexed { index, path ->
                    menuItems.add(
                        MenuItemModel(path).apply {
                            isChecked = filesVM.root == path
                            title = "$usbStorageText ${index + 1}"
                            iconId = R.drawable.usb
                        }
                    )
                }
            }
            
            // App Storage
            menuItems.add(
                MenuItemModel(FileSystemHelper.getExternalFilesDirPath(context)).apply {
                    isChecked = filesVM.type == FilesType.APP
                    title = fileTransferAssistantText
                    iconId = R.drawable.app_icon
                }
            )
            
            menuItems
        }
        
        items.forEach { item ->
            options.add(
                RadioDialogOption(
                    text = item.title,
                    selected = item.isChecked
                ) {
                    sendEvent(DrawerMenuItemClickedEvent(item))
                }
            )
        }
    }
    
    RadioDialog(
        title = stringResource(R.string.folders),
        options = options,
        onDismissRequest = onDismiss
    )
} 