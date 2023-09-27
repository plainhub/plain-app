package com.ismartcoding.plain.ui.exchange

import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.DExchangeRate
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.ResourceHelper

class SelectCurrencyDialog(val onSelect: (DExchangeRate) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    private var searchQ: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.run {
            initMenu(R.menu.select_item)

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    search()
                }
            }
        }

        binding.list.page.setEnableRefresh(false).setEnableAutoLoadMore(false)
        binding.list.rv.linear().setup {
            addType<DExchangeRate>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<DExchangeRate>()
                binding.setStartIcon(ResourceHelper.getCurrencyFlagResId(context, m.currency))
                binding.setKeyText(m.currency)
                binding.setClick {
                    dismiss()
                    onSelect(m)
                }
            }
        }
        search()
    }

    private fun search() {
        UIDataCache.current().latestExchangeRates?.rates?.filter {
            searchQ.isEmpty() || it.currency.contains(searchQ, true)
        }?.sortedBy { it.currency }?.let {
            binding.list.rv.models = it
        }
    }
}
