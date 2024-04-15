package com.ismartcoding.plain.ui.file

import android.content.Context
import android.view.MenuItem
import androidx.lifecycle.LifecycleCoroutineScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preference.FileSortByPreference
import com.ismartcoding.plain.preference.ShowHiddenFilesPreference
import com.ismartcoding.plain.databinding.DialogFilesBinding
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.EditValueDialog
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.extensions.unhighlightTitle
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import kotlinx.coroutines.launch

object FilesTopMenuHelper {
    fun onMenuItemClick(
        lifecycleScope: LifecycleCoroutineScope,
        context: Context,
        viewModel: FilesViewModel,
        binding: DialogFilesBinding,
        menuItem: MenuItem,
    ) {
        when (menuItem.itemId) {
            R.id.create_folder -> {
                EditValueDialog(getString(R.string.create_folder), hint = getString(R.string.name)) {
                    val name = this.binding.value.text
                    lifecycleScope.launch {
                        blockFormUI()
                        withIO { FileSystemHelper.createDirectory(viewModel.path + "/" + name) }
                        dismiss()
                        binding.list.page.refresh()
                    }
                }.show()
            }

            R.id.create_file -> {
                EditValueDialog(getString(R.string.create_file), hint = getString(R.string.name)) {
                    val name = this.binding.value.text
                    lifecycleScope.launch {
                        blockFormUI()
                        withIO { FileSystemHelper.createFile(viewModel.path + "/" + name) }
                        dismiss()
                        binding.list.page.refresh()
                    }
                }.show()
            }

            R.id.show_hidden -> {
                lifecycleScope.launch {
                    withIO { ShowHiddenFilesPreference.putAsync(context, !menuItem.isChecked) }
                    menuItem.isChecked = !menuItem.isChecked
                    binding.list.page.refresh()
                }
            }

            R.id.sort_newest_first -> {
                sort(context, lifecycleScope, binding, FileSortBy.DATE_DESC)
            }

            R.id.sort_oldest_first -> {
                sort(context, lifecycleScope, binding, FileSortBy.DATE_ASC)
            }

            R.id.sort_largest_first -> {
                sort(context, lifecycleScope, binding, FileSortBy.SIZE_DESC)
            }

            R.id.sort_smallest_first -> {
                sort(context, lifecycleScope, binding, FileSortBy.SIZE_ASC)
            }

            R.id.sort_name_asc -> {
                sort(context, lifecycleScope, binding, FileSortBy.NAME_ASC)
            }

            R.id.sort_name_desc -> {
                sort(context, lifecycleScope, binding, FileSortBy.NAME_DESC)
            }
            // select mode menu
            R.id.select_all -> {
                val rv = binding.list.rv
                if (menuItem.title == getString(R.string.select_all)) {
                    menuItem.setTitle(R.string.unselect_all)
                    rv.bindingAdapter.checkedAll(true)
                } else {
                    menuItem.setTitle(R.string.select_all)
                    rv.bindingAdapter.checkedAll(false)
                }
            }
        }
    }

    private fun sort(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        binding: DialogFilesBinding,
        sortBy: FileSortBy,
    ) {
        lifecycleScope.launch {
            val menu = binding.toolbar.menu
            FileSortHelper.getSelectedSortItem(menu, FileSortByPreference.getValueAsync(context)).unhighlightTitle()
            withIO { FileSortByPreference.putAsync(context, sortBy) }
            FileSortHelper.getSelectedSortItem(menu, FileSortByPreference.getValueAsync(context)).highlightTitle(context)
            binding.list.page.refresh()
        }
    }
}
