package com.ismartcoding.plain.ui.file

import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getParentPath
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.ui.models.BaseItemsModel
import com.ismartcoding.plain.ui.views.BreadcrumbItem

class FilesViewModel : BaseItemsModel() {
    var root = FileSystemHelper.getInternalStoragePath()
    var path = root
    val breadcrumbs = mutableListOf(BreadcrumbItem(FileSystemHelper.getInternalStorageName(MainApp.instance), root))
    var cutFiles = mutableListOf<DFile>()
    var copyFiles = mutableListOf<DFile>()
    var type: FilesType = FilesType.INTERNAL_STORAGE

    fun getAndUpdateSelectedIndex(): Int {
        var index = breadcrumbs.indexOfFirst { it.path == path }
        if (index == -1) {
            val parent = path.getParentPath()
            breadcrumbs.reversed().forEach { b ->
                if (b.path != parent && !("$parent/").startsWith(b.path + "/")) {
                    breadcrumbs.remove(b)
                }
            }
            breadcrumbs.add(BreadcrumbItem(path.getFilenameFromPath(), path))
            index = breadcrumbs.size - 1
        }

        return index
    }
}

enum class FilesType {
    INTERNAL_STORAGE,
    RECENTS,
    SDCARD,
    USB_STORAGE,
    APP,
}