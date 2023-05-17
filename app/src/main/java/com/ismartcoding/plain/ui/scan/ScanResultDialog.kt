package com.ismartcoding.plain.ui.scan

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.ismartcoding.lib.extensions.setTextWithLinkSupport
import com.ismartcoding.plain.databinding.DialogScanResultBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.helpers.WebHelper

class ScanResultDialog(val content: String, val callback: () -> Unit) : BaseBottomSheetDialog<DialogScanResultBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.setTextWithLinkSupport(content) {
            WebHelper.open(requireContext(), it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback()
    }
}