package com.ismartcoding.plain.ui.helpers

import android.view.MenuItem
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogListDrawerBinding
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import com.ismartcoding.plain.ui.tag.SelectTagsDialog

object BottomMenuHelper {
    fun onMenuItemClick(
        viewModel: FilteredItemsViewModel,
        binding: DialogListDrawerBinding,
        menuItem: MenuItem,
    ) {
        val list = binding.list
        val rv = list.rv
        when (menuItem.itemId) {
            R.id.add_to_tags -> {
                rv.ensureSelect { items ->
                    SelectTagsDialog(
                        viewModel.dataType,
                        items.map {
                            TagRelationStub.create(it.data)
                        },
                    ).show()
                }
            }
            R.id.remove_from_tag -> {
                rv.ensureSelect { items ->
                    SelectTagsDialog(
                        viewModel.dataType,
                        items.map {
                            TagRelationStub.create(it.data)
                        },
                        removeFromTags = true,
                    ).show()
                }
            }
        }
    }
}
