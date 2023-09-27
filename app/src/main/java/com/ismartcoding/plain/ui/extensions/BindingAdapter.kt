package com.ismartcoding.plain.ui.extensions

import androidx.databinding.BaseObservable
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.item.ItemCheckable
import com.ismartcoding.plain.R

fun BindingAdapter.checkable(
    onItemClick: BindingAdapter.BindingViewHolder.() -> Unit,
    onChecked: () -> Unit,
) {
    R.id.container.onClick {
        if (!toggleMode) {
            onItemClick()
        } else {
            checkedSwitch(bindingAdapterPosition)
        }
    }

    R.id.cb.checkable()

    onChecked { position, isChecked, _ ->
        val m = getModel<ItemCheckable>(position)
        m.isChecked = isChecked
        if (m is BaseObservable) {
            m.notifyChange()
        }
        onChecked()
    }

    onToggle { position, toggleMode, end ->
        val m = getModel<ItemCheckable>(position)
        m.toggleMode = toggleMode
        if (m is BaseObservable) {
            m.notifyChange()
        }
        if (end) {
            if (!toggleMode) checkedAll(false)
        }
    }
}
