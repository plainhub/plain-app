package com.ismartcoding.plain.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.gyf.immersionbar.ImmersionBar
import com.ismartcoding.lib.extensions.immersionBar
import com.ismartcoding.lib.extensions.statusBarHeight
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.theme.AppThemeHelper
import com.ismartcoding.plain.ui.helpers.FragmentHelper
import com.ismartcoding.plain.ui.views.appbarpull.CustomAppBarLayout
import com.ismartcoding.plain.ui.views.appbarpull.QuickNav
import kotlin.math.abs

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

    protected fun setTransparentBar(view: View) {
        val appBar = view.findViewById<CustomAppBarLayout>(R.id.top_app_bar)
        val toolbar = appBar?.findViewById<MaterialToolbar>(R.id.toolbar)
        immersionBar {
            transparentBar()
            titleBar(toolbar)
            statusBarDarkFont(!AppThemeHelper.isDarkMode())
        }

        appBar?.findViewById<QuickNav>(R.id.quick_nav)?.updatePadding(0, statusBarHeight, 0, 0)

        appBar?.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            // if ScrollBehavior is disabled, should not hide status bar
            if ((toolbar?.layoutParams as? AppBarLayout.LayoutParams)?.scrollFlags == 0) {
                return@addOnOffsetChangedListener
            }
            if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
                dialog?.window?.let {
                    ImmersionBar.hideStatusBar(it)
                }
            } else if (verticalOffset == 0) {
                dialog?.window?.let {
                    ImmersionBar.showStatusBar(it)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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