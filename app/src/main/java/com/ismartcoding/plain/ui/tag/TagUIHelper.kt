package com.ismartcoding.plain.ui.tag

import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.models.DrawerMenuGroup
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import com.ismartcoding.plain.ui.models.MenuItemModel
import kotlinx.coroutines.launch

object TagUIHelper {
    suspend fun createMenuGroupAsync(viewModel: FilteredItemsViewModel): DrawerMenuGroup {
        val tagCountMap = TagHelper.count(viewModel.dataType).associate { it.id to it.count }
        val tags = TagHelper.getAll(viewModel.dataType).map { tag ->
            val count = tagCountMap[tag.id] ?: 0
            MenuItemModel(tag).apply {
                isChecked = (viewModel.data as? DTag)?.id == tag.id
                title = tag.name
                iconId = R.drawable.ic_tag
                this.count = count
            }
        }

        return DrawerMenuGroup(getString(R.string.tags)).apply {
            itemSublist = tags
            iconClick = {
                EditValueDialog(getString(R.string.new_tag), getString(R.string.name)) {
                    val value = this.binding.value.text
                    lifecycleScope.launch {
                        blockFormUI()
                        val id = withIO {
                            TagHelper.addOrUpdate("") {
                                name = value
                                type = viewModel.dataType.value
                            }
                        }
                        sendEvent(ActionEvent(ActionSourceType.TAG, ActionType.CREATED, setOf(id)))
                        dismiss()
                    }
                }.show()
            }
        }
    }

    fun itemLongClick(
        itemId: Int, tag: DTag
    ) {
        when (itemId) {
            R.id.edit -> {
                EditValueDialog(getString(R.string.edit_tag), getString(R.string.name), tag.name) {
                    val value = this.binding.value.text
                    coMain {
                        blockFormUI()
                        withIO {
                            TagHelper.addOrUpdate(tag.id) {
                                name = value
                            }
                        }
                        tag.name = value
                        sendEvent(ActionEvent(ActionSourceType.TAG, ActionType.UPDATED, setOf(tag.id)))
                        dismiss()
                    }
                }.show()
            }
            R.id.delete -> {
                coMain {
                    withIO { TagHelper.deleteTagRelationsByTagId(tag.id) }
                    withIO { TagHelper.delete(tag.id) }
                    sendEvent(ActionEvent(ActionSourceType.TAG, ActionType.DELETED, setOf(tag.id)))
                }
            }
        }
    }
}