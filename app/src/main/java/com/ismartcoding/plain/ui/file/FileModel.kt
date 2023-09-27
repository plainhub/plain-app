package com.ismartcoding.plain.ui.file

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.models.BaseItemModel
import com.ismartcoding.plain.ui.models.IDataModel

data class FileModel(override val data: DFile) : IDataModel, BaseItemModel() {
    var startIconId: Int = 0
    var image: String = ""
    var title: String = ""
    var subtitle: String = ""
}
