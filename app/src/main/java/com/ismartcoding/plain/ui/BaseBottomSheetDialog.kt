package com.ismartcoding.plain.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ismartcoding.plain.data.IFormItem
import com.ismartcoding.plain.ui.helpers.FragmentHelper
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseBottomSheetDialog<VB : ViewBinding> : BottomSheetDialogFragment() {
    private var _binding: VB? = null
    val binding get() = _binding!!

    val isActive: Boolean
        get() = view != null

    private val mFormItems = mutableListOf<IFormItem>()
    private var isKeyboardClosed = true
    private var isKeyPressedFromHardwareKeyboard = false

    protected open fun getSubmitButton(): LoadingButtonView? {
        return null
    }

    protected fun addFormItem(vararg formItems: IFormItem) {
        formItems.forEach {
            mFormItems.add(it)
        }
    }

    protected fun onKeyboardChanged(onClosed: () -> Unit) {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight = binding.root.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                // keyboard is showing
                isKeyboardClosed = false
                isKeyPressedFromHardwareKeyboard = false
            } else {
                if (!isKeyboardClosed) {
                    if (!isKeyPressedFromHardwareKeyboard) {
                        lifecycleScope.launch {
                            delay(100)
                            onClosed()
                        }
                    }
                    isKeyboardClosed = true
                }
            }
        }
    }

    protected fun hasInputError(): Boolean {
        mFormItems.forEach {
            it.beforeSubmit()
        }
        return mFormItems.any { (it as View).isShown && it.hasError }
    }

    private fun blurAllInputs() {
        mFormItems.forEach {
            it.blurAndHideSoftInput()
        }
    }

    fun blockFormUI() {
        isCancelable = false
        getSubmitButton()?.showLoading()
    }

    fun unblockFormUI() {
        isCancelable = true
        getSubmitButton()?.hideLoading()
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onKeyboardChanged {
            blurAllInputs()
        }
    }

    // https://stackoverflow.com/questions/57647751/android-databinding-is-leaking-memory
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun show() {
        super.show(MainActivity.instance.get()!!.supportFragmentManager, this.javaClass.simpleName)
    }
}
