package com.ismartcoding.plain.ui.feed

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.databinding.DialogAddFeedBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.LoadingButtonView
import com.ismartcoding.plain.workers.FeedFetchWorker
import kotlinx.coroutines.launch

class AddFeedDialog() : BaseBottomSheetDialog<DialogAddFeedBinding>() {
    override fun getSubmitButton(): LoadingButtonView {
        return binding.button
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        addFormItem(binding.url)
    }

    private fun updateUI() {
        binding.button.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                val url = binding.url.text
                blockFormUI()
                if (withIO { FeedHelper.getByUrl(url) } != null) {
                    DialogHelper.showMessage(R.string.already_added)
                    unblockFormUI()
                    return@launch
                }

                try {
                    val syndFeed = withIO { FeedHelper.fetchAsync(url) }
                    val id =
                        withIO {
                            FeedHelper.addAsync {
                                this.url = url
                                this.name = syndFeed.title ?: ""
                            }
                        }
                    FeedFetchWorker.oneTimeRequest(id)
                    sendEvent(ActionEvent(ActionSourceType.FEED, ActionType.CREATED, setOf(id)))
                    dismiss()
                } catch (ex: Throwable) {
                    unblockFormUI()
                    LogCat.e(ex)
                    DialogHelper.showErrorDialog(requireContext(), ex.toString())
                }
            }
        }
    }
}
