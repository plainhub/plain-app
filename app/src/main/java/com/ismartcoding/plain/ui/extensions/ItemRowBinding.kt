package com.ismartcoding.plain.ui.extensions

import android.widget.CompoundButton
import com.ismartcoding.lib.extensions.setSelectableItemBackground
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ItemRowBinding

fun ItemRowBinding.setSwitch(
    enable: Boolean,
    onChanged: ((CompoundButton, Boolean) -> Unit)? = null,
): ItemRowBinding {
    endSwitch.setOnCheckedChangeListener(null)
    endSwitch.isChecked = enable
    val listener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> onChanged?.invoke(buttonView, isChecked) }
    endSwitch.setOnCheckedChangeListener(listener)
    endSwitch.tag = listener
    return this
}

fun ItemRowBinding.initTheme(): ItemRowBinding {
    val context = container.context
    val color = context.getColor(R.color.primary)
    container.setSelectableItemBackground()
    textKey.setTextColor(color)
    textValue.setTextColor(color)

    return this
}
