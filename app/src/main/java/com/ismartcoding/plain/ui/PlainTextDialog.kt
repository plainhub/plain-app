package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import com.ismartcoding.plain.databinding.DialogPlainTextBinding
import com.ismartcoding.plain.ui.extensions.onBack

class PlainTextDialog(val title: String, val content: String) : BaseDialog<DialogPlainTextBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.topAppBar.toolbar.title = title
        binding.topAppBar.toolbar.onBack {
            dismiss()
        }
        binding.content.text = content
    }
}
