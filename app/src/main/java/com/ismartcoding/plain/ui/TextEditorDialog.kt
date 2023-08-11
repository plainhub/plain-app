package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogTextEditorBinding
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import kotlinx.coroutines.launch
import java.io.File

class TextEditorDialog(val path: String) : BaseDialog<DialogTextEditorBinding>() {
    override fun onBackPressed() {
        if (binding.editor.isChanged()) {
            DialogHelper.confirmToLeave(requireContext()) {
                dismiss()
            }
        } else {
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.toolbar.run {
            title = path.getFilenameFromPath()
            initMenu(R.menu.save)

            onBack {
                onBackPressed()
            }

            onMenuItemClick {
                when (itemId) {
                    R.id.save -> {
                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            withIO { File(path).writeText(binding.editor.getText()) }
                            DialogHelper.hideLoading()
                            dismiss()
                        }
                    }
                }
            }
        }

        setWindowSoftInput(binding.editor)

        lifecycleScope.launch {
            val file = File(path)
            val text = withIO {
                file.readText()
            }
            binding.editor.initViewAsync(
                lifecycle,
                text, path.getFilenameExtension()
            )
        }
    }
}