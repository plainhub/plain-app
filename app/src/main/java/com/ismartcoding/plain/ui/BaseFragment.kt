package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.ismartcoding.plain.ui.helpers.FragmentHelper

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var _binding: VB? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelper.createBinding(this, inflater, container)
        return binding.root
    }

    // https://stackoverflow.com/questions/57647751/android-databinding-is-leaking-memory
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
