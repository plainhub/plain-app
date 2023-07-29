package com.ismartcoding.plain.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.databinding.DialogEditChatTextBinding
import com.ismartcoding.plain.features.UpdateMessageEvent
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick

class EditChatTextDialog(val id: String, val content: String) : BaseBottomSheetDialog<DialogEditChatTextBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.input.setText(content)
        binding.send.setSafeClick {
            val c = binding.input.text.toString()
            if (c.isEmpty()) {
                return@setSafeClick
            }
            sendEvent(UpdateMessageEvent(id, c))
            dismiss()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // fix the glitch bug
            setWindowSoftInput(binding.container)
        }, 200)
    }
}