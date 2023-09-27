package com.ismartcoding.plain.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.ismartcoding.lib.softinput.setWindowSoftInput
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogEditValueBinding
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.views.LoadingButtonView

class EditValueDialog(
    val title: String,
    val hint: String = LocaleHelper.getString(R.string.value),
    val value: String = "",
    private val inputType: Int = -1,
    val callback: (EditValueDialog.() -> Unit)? = null,
) : BaseBottomSheetDialog<DialogEditValueBinding>() {
    override fun getSubmitButton(): LoadingButtonView {
        return binding.button
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.topAppBar.title = title
        binding.value.let {
            if (inputType != -1) {
                it.inputType = inputType
            }
            it.hint = hint
            it.text = value
        }
        binding.button.setSafeClick {
            callback?.invoke(this)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            setWindowSoftInput(binding.button)
        }, 200)
    }
}
