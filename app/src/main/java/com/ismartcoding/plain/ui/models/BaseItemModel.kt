package com.ismartcoding.plain.ui.models

import androidx.databinding.BaseObservable
import com.ismartcoding.lib.brv.item.ItemCheckable

abstract class BaseItemModel : ItemCheckable, BaseObservable() {
    override var toggleMode = false
    override var isChecked = false

    var swipeEnable: Boolean = false
    var leftSwipeText: String = ""
    var rightSwipeText: String = ""
    var leftSwipeClick: (() -> Unit)? = null
    var rightSwipeClick: (() -> Unit)? = null
}
