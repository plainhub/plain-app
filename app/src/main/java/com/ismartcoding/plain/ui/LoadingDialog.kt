package com.ismartcoding.plain.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.ismartcoding.plain.databinding.DialogLoadingBinding

class LoadingDialog(val message: String = "") : DialogFragment() {
    private lateinit var binding: DialogLoadingBinding

    fun updateMessage(message: String) {
        binding.message.text = message
        binding.message.isVisible = message.isNotEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogLoadingBinding.inflate(inflater, container, false)
        dialog?.window?.run {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
        }
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateMessage(message)
    }

    fun show() {
        MainActivity.instance.get()?.supportFragmentManager?.let {
            super.show(it, this.javaClass.simpleName)
        }
    }
}
