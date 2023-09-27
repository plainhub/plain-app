package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.ismartcoding.plain.databinding.DialogTabsPageBinding
import com.ismartcoding.plain.ui.helpers.TabsPageHelper
import com.ismartcoding.plain.ui.models.BaseItemsModel

abstract class BaseTagsPageDialog : BaseDialog<DialogTabsPageBinding>() {
    abstract val viewModel: BaseItemsModel
    abstract val tabTitleIds: List<Int>
    abstract val tagFragments: List<Fragment>

    override fun onBackPressed() {
        if (viewModel.toggleMode.value == true && binding.pager.currentItem == 0) {
            viewModel.toggleMode.value = false
        } else {
            dismiss()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.dismissed.observe(viewLifecycleOwner) {
            if (it) {
                dismiss()
            }
        }

        TabsPageHelper.initTabsPager(this, tabTitleIds, tagFragments)
    }
}
