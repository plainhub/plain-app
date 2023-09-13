package com.ismartcoding.plain.ui.tag

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.databinding.DialogSelectTagsBinding
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.tag.TagHelper
import com.ismartcoding.plain.features.tag.TagRelationStub
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.models.ListItemModel
import kotlinx.coroutines.launch

class SelectTagsDialog(
    val type: DataType,
    val items: List<TagRelationStub>,
    val removeFromTags: Boolean = false,
) : BaseBottomSheetDialog<DialogSelectTagsBinding>() {
    data class TagModel(val tag: DTag) : ListItemModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.run {
            title = getString(if (removeFromTags) R.string.remove_from_tags else R.string.add_to_tags)
            initMenu(R.menu.tags)
            onMenuItemClick {
                when (itemId) {
                    R.id.add -> {
                        EditValueDialog(getString(R.string.add_tag), getString(R.string.name)) {
                            val value = this.binding.value.text
                            lifecycleScope.launch {
                                blockFormUI()
                                withIO {
                                    TagHelper.addOrUpdate("") {
                                        name = value
                                        type = this@SelectTagsDialog.type.value
                                    }
                                }
                                dismiss()
                                this@SelectTagsDialog.binding.list.page.refresh()
                            }
                        }.show()
                    }
                }
            }
        }

        binding.list.rv.linear().setup {
            addType<TagModel>(R.layout.item_row)
            R.id.container.onClick {
                lifecycleScope.launch {
                    val m = getModel<TagModel>()
                    if (items.size == 1) {
                        if (m.isSelected()) {
                            withIO { TagHelper.deleteTagRelationByKeysTagId(items.map { it.key }.toSet(), m.tag.id) }
                            m.showSelected(false)
                        } else {
                            withIO {
                                TagHelper.addTagRelations(items.map {
                                    it.toTagRelation(m.tag.id, type)
                                })
                            }
                            m.showSelected(true)
                        }
                        m.notifyChange()
                        notifyItemChanged(modelPosition)
                        sendEvent(ActionEvent(ActionSourceType.TAG_RELATION, ActionType.UPDATED, setOf(m.tag.id)))
                    } else if (removeFromTags) {
                        DialogHelper.showLoading()
                        val ids = items.map { it.key }.toSet()
                        withIO {
                            TagHelper.deleteTagRelationByKeysTagId(
                                ids,
                                m.tag.id
                            )
                        }
                        DialogHelper.hideLoading()
                        dismiss()
                        DialogHelper.showMessage(R.string.updated)
                        sendEvent(ActionEvent(ActionSourceType.TAG_RELATION, ActionType.DELETED, ids))
                    } else {
                        DialogHelper.showLoading()
                        val existingKeys = withIO { TagHelper.getKeysByTagId(m.tag.id) }
                        val newItems = items.filter { !existingKeys.contains(it.key) }
                        if (newItems.isNotEmpty()) {
                            withIO {
                                TagHelper.addTagRelations(newItems.map {
                                    it.toTagRelation(m.tag.id, type)
                                })
                            }
                        }
                        DialogHelper.hideLoading()
                        dismiss()
                        DialogHelper.showMessage(R.string.updated)
                        sendEvent(ActionEvent(ActionSourceType.TAG_RELATION, ActionType.UPDATED, setOf(m.tag.id)))
                    }
                }
            }
        }

        binding.list.page.onRefresh {
            search()
        }.showLoading()
    }

    private fun search() {
        lifecycleScope.launch {
            val tags = withIO { TagHelper.getAll(type) }
            val tagIds = tags.map { it.id }
            val tagRelations = if (items.size == 1) {
                withIO { TagHelper.getTagRelationsByKey(items.first().key, type) }.filter { tagIds.contains(it.tagId) }
            } else arrayListOf()
            binding.list.page.addData(tags
                .map { tag ->
                    TagModel(tag).apply {
                        keyText = tag.name
                        if (items.size == 1) {
                            showSelected(tagRelations.any { it.tagId == tag.id })
                        }
                    }
                })
        }
    }
}