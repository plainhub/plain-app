package com.ismartcoding.plain.ui.models

import android.view.View
import com.ismartcoding.lib.brv.item.ItemExpand

class DrawerMenuGroup(val title: String) : ItemExpand {
    override var itemGroupPosition: Int = 0
    override var itemExpand: Boolean = true
    override var itemSublist: List<Any> = arrayListOf()
    var iconClick: ((View) -> Unit)? = null
}

enum class DrawerMenuGroupType {
    ALL,
    TRASH,
    CALL_TYPES,
    SMS_TYPES,
    FOLDERS,
    TAGS,
}
