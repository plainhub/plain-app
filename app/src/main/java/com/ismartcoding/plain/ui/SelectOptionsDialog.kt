package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.ISelectOption
import com.ismartcoding.plain.databinding.DialogSelectOptionsBinding
import com.ismartcoding.plain.ui.models.ListItemModel
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class SelectOptionsDialog<T : ISelectOption>(
    private val title: String,
    private val options: List<T>,
    val selectCallback: (T) -> Unit,
) : BaseBottomSheetDialog<DialogSelectOptionsBinding>() {
    data class ItemModel(val data: ISelectOption) : ListItemModel()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.title = title
        initList()
        updateList()
    }

    private fun initList() {
        binding.rv.linear().setup {
            addType<ItemModel>(R.layout.item_row)
            R.id.container.onClick {
                val m = getModel<ItemModel>()
                selectCallback(m.data as T)
                dismiss()
            }
        }
    }

    private fun updateList() {
        lifecycleScope.launch {
            val context = requireContext()
            binding.rv.models =
                options.map { opt ->
                    ItemModel(opt).apply {
                        keyText = opt.getText()
                        showSelected(withIO { opt.isSelected(context) })
                    }
                }
        }
    }
}
