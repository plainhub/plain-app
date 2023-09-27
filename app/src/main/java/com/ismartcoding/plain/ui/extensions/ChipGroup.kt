package com.ismartcoding.plain.ui.extensions

import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.ismartcoding.plain.ui.views.ChipItem

fun ChipGroup.initView(
    items: List<ChipItem>,
    value: String,
    ensureMinTouchTargetSize: Boolean = true,
    onChanged: (String) -> Unit,
) {
    if (childCount > 0) {
        removeAllViews()
    }

    val chips = mutableListOf<Chip>()
    items.forEach { chipItem ->
        val chip = chipItem.createView(context)
        chip.isChecked = chipItem.value == value
        chip.setEnsureMinTouchTargetSize(ensureMinTouchTargetSize)
        chips.add(chip)
        addView(chip)
    }

    var index = items.indexOfFirst { it.value == value }
    if (index == -1) {
        index = 0
    }

    setOnCheckedStateChangeListener { _, checkedIds ->
        if (checkedIds.isEmpty()) {
            chips[index].isChecked = true
        } else {
            index = chips.indexOfFirst { it.isChecked }
            onChanged(findViewById<Chip>(checkedIds[0]).tag as String)
        }
    }
}

fun ChipGroup.getValue(): String {
    return children.find { it is Chip && it.isChecked }?.tag as? String ?: ""
}
