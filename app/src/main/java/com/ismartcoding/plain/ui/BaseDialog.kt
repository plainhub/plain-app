package com.ismartcoding.plain.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.FragmentHelper

abstract class BaseDialog<VB : ViewBinding> : DialogFragment() {
    private var _binding: VB? = null
    val binding get() = _binding!!

    val isActive: Boolean
        get() = view != null

    override fun getTheme(): Int {
        return R.style.Theme_Plain
    }

    protected open fun onBackPressed() {
        dismiss()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                this@BaseDialog.onBackPressed()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelper.createBinding(this, inflater, container)
        return binding.root
    }

    fun show() {
        super.show(MainActivity.instance.get()!!.supportFragmentManager, this.javaClass.simpleName)
    }

    // https://stackoverflow.com/questions/57647751/android-databinding-is-leaking-memory
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
