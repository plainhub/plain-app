package com.ismartcoding.plain.ui.feed

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.databinding.DialogEditFeedBinding
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.addTextRow
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.launch

class EditFeedDialog(val data: DFeed) : BaseBottomSheetDialog<DialogEditFeedBinding>() {
    override fun getSubmitButton(): LoadingButtonView {
        return binding.btn
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        addFormItem(binding.name)
    }

    private fun updateUI() {
        binding.name.text = data.name
        binding.url.setKeyText(R.string.url)
        binding.url.addTextRow(data.url)
        binding.btn.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                val name = binding.name.text
                blockFormUI()
                withIO {
                    FeedHelper.updateAsync(data.id) {
                        this.name = name
                    }
                }
                data.name = name
                unblockFormUI()
                sendEvent(ActionEvent(ActionSourceType.FEED, ActionType.UPDATED, setOf(data.id), data))
                dismiss()
            }
        }
    }
}
