package com.ismartcoding.plain.ui.preview.utils

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ismartcoding.plain.ui.preview.PreviewDialog

internal object ViewModelUtils {
    fun <T : ViewModel> provideViewModel(
        view: View,
        modelClass: Class<T>,
    ): T? {
        return (view.activity as? FragmentActivity?)?.supportFragmentManager?.fragments?.find { it is PreviewDialog }
            ?.let { ViewModelProvider(it).get(modelClass) }
    }
}
