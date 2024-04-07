package com.ismartcoding.plain.ui.endict

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.VocabularyQuery
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.databinding.DialogCreateVocabularyBinding
import com.ismartcoding.plain.features.VocabularyCreatedEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.vocabulary.VocabularyList
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.launch

class CreateVocabularyDialog : BaseBottomSheetDialog<DialogCreateVocabularyBinding>() {
    override fun getSubmitButton(): LoadingButtonView {
        return binding.button2
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.template.selectValue = "none"
        updateTemplateRow()
        binding.button2.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                doCreateAsync()
            }
        }
        addFormItem(binding.name)
        addFormItem(binding.template)
    }

    private suspend fun doCreateAsync() {
        blockFormUI()
        val template = binding.template.selectValue
        if (template == "none") {
            addOrUpdateDbAsync(setOf())
            dismiss()
            return
        }

        val r =
            withIO {
                BoxApi.mixQueryAsync(VocabularyQuery(template))
            }

        if (!r.isSuccess()) {
            unblockFormUI()
            DialogHelper.showErrorDialog(r.getErrorMessage())
            return
        }

        r.response?.data?.vocabulary?.let {
            addOrUpdateDbAsync(it.words.toSet())
        }

        dismiss()
    }

    private suspend fun addOrUpdateDbAsync(words: Set<String>) {
        withIO {
            VocabularyList.addOrUpdateAsync("") {
                boxId = TempData.selectedBoxId
                name = binding.name.text
                this.words = ArrayList(words.distinct())
            }
        }
        sendEvent(VocabularyCreatedEvent())
    }

    private fun updateTemplateRow() {
        binding.template.run {
            setValueText(LocaleHelper.getString("vocabulary_$selectValue"))
            setClick {
                SelectTemplateDialog { id ->
                    selectValue = id
                    updateTemplateRow()
                }.show()
            }
        }
    }
}
