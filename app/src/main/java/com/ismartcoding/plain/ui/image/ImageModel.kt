package com.ismartcoding.plain.ui.image

import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.ui.models.BaseItemModel
import com.ismartcoding.plain.ui.models.IDataModel

data class ImageModel(override val data: DImage) : IDataModel, BaseItemModel() {
    var title: String = ""
    var subtitle: String = ""
    var size: String = ""
}
