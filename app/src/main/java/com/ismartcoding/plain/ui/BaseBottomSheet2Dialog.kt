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

abstract class BaseBottomSheet2Dialog() : BottomSheetDialogFragment() {
    fun show() {
        super.show(MainActivity.instance.get()!!.supportFragmentManager, this.javaClass.simpleName)
    }
}