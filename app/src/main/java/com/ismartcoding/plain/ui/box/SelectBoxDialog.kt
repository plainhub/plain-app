package com.ismartcoding.plain.ui.box

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogSelectBoxBinding
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.db.DBox
import com.ismartcoding.plain.extensions.*
import com.ismartcoding.plain.features.CurrentBoxChangedEvent
import com.ismartcoding.plain.features.box.BoxHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class SelectBoxDialog() : BaseBottomSheetDialog<DialogSelectBoxBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.rv.linear().setup {
                addType<DBox>(R.layout.view_list_item)
                onBind {
                    val binding = getBinding<ViewListItemBinding>()
                    val m = getModel<DBox>()
                    binding.clearTextRows()
                    binding.setKeyText(m.name)
                    binding.addTextRow(if (m.ips.isEmpty()) getString(R.string.unknown_ip) else m.ips.joinToString(", "))
                    binding.addTextRow(getString(R.string.added_at) + " " + m.createdAt.formatDateTime())
                    if (TempData.selectedBoxId == m.id) {
                        binding.showSelected()
                    } else {
                        binding.hideEndIcon()
                    }
                    binding.enableSwipeMenu(true)
                    binding.setRightSwipeButton(getString(R.string.delete)) {
                        DialogHelper.confirmToAction(requireContext(), R.string.confirm_to_delete) {
                            lifecycleScope.launch {
                                withIO {
                                    BoxHelper.unpairAsync(m)
                                }
                                updateList()
                                sendEvent(CurrentBoxChangedEvent())
                            }
                        }
                    }
                    binding.setLeftSwipeButton(getString(R.string.view)) {
                        BoxDetailDialog(m.id).show()
                    }
                    binding.setClick {
                        lifecycleScope.launch {
                            val isChanged = TempData.selectedBoxId != m.id
                            TempData.selectedBoxId = m.id
                            UIDataCache.current().box =
                                withIO {
                                    BoxHelper.getSelectedBoxAsync()
                                }
                            if (isChanged) {
                                sendEvent(CurrentBoxChangedEvent())
                            }
                            dismiss()
                        }
                    }
                }
            }
            updateList()
            binding.addNewBox.setSafeClick {
                AddNewBoxDialog {
                    lifecycleScope.launch {
                        updateList()
                        sendEvent(CurrentBoxChangedEvent())
                    }
                }.show()
            }
        }
    }

    private suspend fun updateList() {
        val items =
            withIO {
                BoxHelper.getItemsAsync()
            }
        binding.rv.models = items
    }
}
