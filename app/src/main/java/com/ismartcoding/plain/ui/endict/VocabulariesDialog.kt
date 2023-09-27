package com.ismartcoding.plain.ui.endict

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogVocabulariesBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.db.DVocabulary
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.vocabulary.VocabularyList
import com.ismartcoding.plain.ui.BaseDialog
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class VocabulariesDialog : BaseDialog<DialogVocabulariesBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.onBack {
            dismiss()
        }

        binding.list.rv.linear().setup {
            addType<DVocabulary>(R.layout.view_list_item)
            onBind {
                val binding = getBinding<ViewListItemBinding>()
                val m = getModel<DVocabulary>()
                binding.setKeyText(m.name)
                binding.clearTextRows()
                binding.addTextRow(LocaleHelper.getStringF(R.string.count_words, "count", m.words.size))
                binding.enableSwipeMenu(true)

                binding.setLeftSwipeButton(getString(R.string.edit)) {
                    EditValueDialog(m.name, getString(R.string.name), m.name) {
                        val value = this.binding.value.text
                        lifecycleScope.launch {
                            blockFormUI()
                            withIO {
                                VocabularyList.addOrUpdateAsync(m.id) {
                                    name = value
                                }
                            }
                            dismiss()
                            sendEvent(VocabularyUpdatedEvent())
                        }
                    }.show()
                }
                binding.setRightSwipeButton(getString(R.string.delete)) {
                    DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                        lifecycleScope.launch {
                            withIO { VocabularyList.deleteAsync(m) }
                            binding.swipeMenu.quickClose()
                            sendEvent(VocabularyDeletedEvent(m.id))
                        }
                    }
                }

                binding.setClick {
                    VocabularyDialog(m).show()
                }
            }
        }

        binding.list.page.onRefresh {
            updateList()
        }.showLoading()

        binding.fab.setSafeClick {
            CreateVocabularyDialog().show()
        }

        receiveEvent<VocabularyCreatedEvent> {
            binding.list.page.showLoading()
        }

        receiveEvent<VocabularyUpdatedEvent> {
            binding.list.page.showLoading()
        }

        receiveEvent<VocabularyDeletedEvent> {
            binding.list.page.showLoading()
        }

        receiveEvent<VocabularyWordsDeletedEvent> {
            binding.list.page.showLoading()
        }

        receiveEvent<VocabularyWordsUpdatedEvent> {
            binding.list.page.showLoading()
        }
    }

    private fun updateList() {
        lifecycleScope.launch {
            binding.list.page.addData(
                withIO {
                    VocabularyList.getItemsAsync()
                },
            )
        }
    }
}
