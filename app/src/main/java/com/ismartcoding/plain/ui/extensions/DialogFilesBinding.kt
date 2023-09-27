package com.ismartcoding.plain.ui.extensions

import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.extensions.px
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogFilesBinding
import com.ismartcoding.plain.ui.file.FilesViewModel

fun DialogFilesBinding.initToggleMode(
    viewLifecycleOwner: LifecycleOwner,
    viewModel: FilesViewModel,
) {
    viewModel.toggleMode.observe(viewLifecycleOwner) { toggleMode ->
        list.rv.bindingAdapter.toggle(toggleMode)
        if (toggleMode) {
            layout.setExpanded(true)
            toolbar.setScrollBehavior(false)
            bottomAction.isVisible = true
            val context = list.page.context
            list.page.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = context.px(R.dimen.size_hhl)
            }
            bottomAction.performShow()
        } else {
            toolbar.setScrollBehavior(true)
            list.page.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = 0
            }
            bottomAction.performHide()
        }
        toolbar.menu.run {
            findItem(R.id.select_all)?.isVisible = toggleMode
            findItem(R.id.select_all)?.setTitle(R.string.select_all)
            findItem(R.id.search)?.isVisible = !toggleMode
            findItem(R.id.more)?.isVisible = !toggleMode
        }
        toolbar.toggleNavigationIcon(toggleMode)
        toolbar.updateFilesTitle(viewModel, list.rv)
    }
}
