package com.ismartcoding.plain.ui.endict

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogSelectItemBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.db.DVocabulary
import com.ismartcoding.plain.features.vocabulary.VocabularyList
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText
import kotlinx.coroutines.launch

class SelectVocabularyDialog(val excludeIds: Set<String>, val onSelect: (DVocabulary) -> Unit) : BaseBottomSheetDialog<DialogSelectItemBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.rv.isNestedScrollingEnabled = false
        binding.list.rv.linear().setup {
            addType<DVocabulary>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<DVocabulary>()
                binding.setKeyText(m.name)
                binding.setClick {
                    onSelect(m)
                    dismiss()
                }
            }
        }

        binding.list.page.onRefresh {
            updateList()
        }.showLoading()
    }

    private fun updateList() {
        lifecycleScope.launch {
            binding.list.page.addData(
                withIO {
                    VocabularyList.getItemsAsync().filter { !excludeIds.contains(it.id) }
                },
            )
        }
    }
}
