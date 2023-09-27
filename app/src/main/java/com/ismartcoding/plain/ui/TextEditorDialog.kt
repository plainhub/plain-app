package com.ismartcoding.plain.ui

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.extensions.getFilenameExtension
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogTextEditorBinding
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onBack
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.launch

class TextEditorDialog(val uri: Uri) : BaseDialog<DialogTextEditorBinding>() {
    override fun onBackPressed() {
        if (binding.editor.isChanged()) {
            DialogHelper.confirmToLeave(requireContext()) {
                dismiss()
            }
        } else {
            dismiss()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.toolbar.run {
            title = uri.getFileName(requireContext())
            initMenu(R.menu.save)

            onBack {
                onBackPressed()
            }

            menu.findItem(R.id.save).isVisible = uri.scheme == ContentResolver.SCHEME_FILE

            onMenuItemClick {
                when (itemId) {
                    R.id.save -> {
                        lifecycleScope.launch {
                            DialogHelper.showLoading()
                            withIO { uri.toFile().writeText(binding.editor.getText()) }
                            DialogHelper.hideLoading()
                            dismiss()
                        }
                    }
                }
            }
        }

        setWindowSoftInput(binding.editor)

        lifecycleScope.launch {
            var text = ""
            text =
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    val context = requireContext()
                    withIO {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()
                            ?.use { it.readText() } ?: ""
                    }
                } else {
                    withIO {
                        uri.toFile().readText()
                    }
                }
            val fileExtension = binding.topAppBar.toolbar.title.toString().getFilenameExtension()
            binding.editor.initViewAsync(
                lifecycle,
                text,
                fileExtension,
            )
        }
    }
}
