package com.ismartcoding.plain.ui.extensions

import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LifecycleOwner
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.databinding.DialogListDrawerBinding
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel

fun DialogListDrawerBinding.initToggleMode(
    viewLifecycleOwner: LifecycleOwner,
    viewModel: FilteredItemsViewModel,
    title: Int,
) {
    viewModel.toggleMode.observe(viewLifecycleOwner) { toggleMode ->
        list.rv.bindingAdapter.toggle(toggleMode)
        val context = list.page.context
        if (toggleMode) {
            topAppBar.layout.setExpanded(true)
            topAppBar.setScrollBehavior(false)
            bottomAction.isVisible = true
            list.rv.setPadding(0, 0, 0, context.dp2px(72))
            bottomAction.performShow()
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            topAppBar.setScrollBehavior(true)
            if (viewModel.dataType != DataType.AUDIO) {
                list.rv.setPadding(0, 0, 0, context.dp2px(32))
            }
            bottomAction.performHide()
            drawer.setDrawerLockMode(
                if (viewModel.data is DMediaBucket) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED,
            )
        }
        topAppBar.toolbar.menu.run {
            findItem(R.id.select_all)?.isVisible = toggleMode
            findItem(R.id.select_all)?.setTitle(R.string.select_all)
            findItem(R.id.search)?.isVisible = !toggleMode
            findItem(R.id.menu)?.isVisible = !toggleMode && viewModel.data !is DMediaBucket
            findItem(R.id.sort)?.isVisible = !toggleMode
            findItem(R.id.more)?.isVisible = !toggleMode
        }
        player.isVisible = !toggleMode && viewModel.dataType == DataType.AUDIO
        topAppBar.toolbar.toggleNavigationIcon(toggleMode)
        topAppBar.toolbar.updateTitle(viewModel, list.rv, title, viewModel.total)
    }
}
