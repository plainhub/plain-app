package com.ismartcoding.plain.ui.book

import androidx.databinding.BaseObservable
import com.ismartcoding.lib.brv.item.ItemCheckable
import com.ismartcoding.plain.db.DBook
import com.ismartcoding.plain.ui.models.IDataModel

class BookModel(override val data: DBook) : IDataModel, ItemCheckable, BaseObservable() {
    override var toggleMode = false
    override var isChecked = false
    var title: String = ""
    var image: String = ""
    var subtitle: String = ""
}
