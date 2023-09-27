package com.ismartcoding.plain.ui.views.texteditor

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.EditorShowLineNumbersPreference
import com.ismartcoding.plain.data.preference.EditorSyntaxHighlightPreference
import com.ismartcoding.plain.data.preference.EditorWrapContentPreference
import com.ismartcoding.plain.databinding.DialogEditorSettingsBinding
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setKeyText
import com.ismartcoding.plain.ui.extensions.setSwitch
import kotlinx.coroutines.launch

class EditorSettingsDialog : BaseBottomSheetDialog<DialogEditorSettingsBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            val context = requireContext()
            binding.lineNumbers.setKeyText(R.string.show_line_numbers)
                .setSwitch(withIO { EditorShowLineNumbersPreference.getAsync(context) }, onChanged = { _, isEnabled ->
                    lifecycleScope.launch {
                        withIO {
                            EditorShowLineNumbersPreference.putAsync(context, isEnabled)
                        }
                        sendEvent(EditorSettingsChangedEvent(EditorSettingsType.LINE_NUMBERS))
                    }
                })
            binding.wrapContent.setKeyText(R.string.wrap_content)
                .setSwitch(withIO { EditorWrapContentPreference.getAsync(context) }, onChanged = { _, isEnabled ->
                    lifecycleScope.launch {
                        withIO { EditorWrapContentPreference.putAsync(context, isEnabled) }
                        sendEvent(EditorSettingsChangedEvent(EditorSettingsType.WRAP_CONTENT))
                    }
                })

            binding.syntaxHighlight.setKeyText(R.string.syntax_highlight)
                .setSwitch(withIO { EditorSyntaxHighlightPreference.getAsync(context) }, onChanged = { _, isEnabled ->
                    lifecycleScope.launch {
                        withIO { EditorSyntaxHighlightPreference.putAsync(context, isEnabled) }
                        sendEvent(EditorSettingsChangedEvent(EditorSettingsType.SYNTAX_HIGHLIGHT))
                    }
                })
        }
    }
}
