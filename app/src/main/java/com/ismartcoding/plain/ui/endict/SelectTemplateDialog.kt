package com.ismartcoding.plain.ui.endict

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.box.FetchVocabulariesEvent
import com.ismartcoding.plain.features.box.VocabulariesResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.helpers.DialogHelper

class SelectTemplateDialog(val onSelect: (String) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<String>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<String>()
                binding.setKeyText(LocaleHelper.getString("vocabulary_$m"))
                binding.setClick {
                    onSelect(m)
                    dismiss()
                }
            }
        }

        binding.list.page.onRefresh {
            fetch()
        }

        receiveEvent<VocabulariesResultEvent> { event ->
            val r = event.result
            if (!r.isSuccess()) {
                DialogHelper.showMessage(r)
                binding.list.page.finishRefresh(false)
                binding.list.page.showError()
                return@receiveEvent
            }
            updateList()
        }

        if (UIDataCache.current().vocabularies == null) {
            binding.list.page.showLoading()
        } else {
            updateList()
        }
    }

    private fun updateList() {
        val items = mutableListOf<String>()
        val sortedIds = listOf("none", "xiaoxue", "chuzhong", "gaozhong", "cet4", "cet6", "kaoyan")
        items.add("none")
        items.addAll(UIDataCache.current().vocabularies?.map { it.id } ?: arrayListOf())
        binding.list.page.replaceData(
            items.sortedBy {
                var index = sortedIds.indexOf(it)
                if (index == -1) {
                    index = 999
                }
                index
            },
        )
    }

    private fun fetch() {
        sendEvent(FetchVocabulariesEvent(TempData.selectedBoxId))
    }
}
