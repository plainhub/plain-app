package com.ismartcoding.plain.ui.extensions

import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.getModelList
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DFeed
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.ui.file.FileModel
import com.ismartcoding.plain.ui.models.BaseItemsModel
import com.ismartcoding.plain.ui.models.DMediaFolders
import com.ismartcoding.plain.ui.models.DType
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel

fun MaterialToolbar.onSearch(callback: (q: String) -> Unit) {
    with(menu.findItem(R.id.search).actionView as SearchView) {
        setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    callback(newText ?: "")
                    return false
                }
            },
        )
    }
}

fun MaterialToolbar.onBack(callback: () -> Unit) {
    setNavigationOnClickListener {
        callback()
    }
}

fun MaterialToolbar.onSearchFocusChange(callback: (hasFocus: Boolean) -> Unit) {
    with(menu.findItem(R.id.search).actionView as SearchView) {
        setOnQueryTextFocusChangeListener { _, hasFocus ->
            callback(hasFocus)
        }
    }
}

fun MaterialToolbar.initMenu(
    @MenuRes menuId: Int,
    overflow: Boolean = false,
) {
    menu.clear()
    inflateMenu(menuId)
    if (overflow) {
        overflowIcon = ContextCompat.getDrawable(this.context, R.drawable.ic_more_vert)
    }
}

fun MaterialToolbar.toggleNavigationIcon(toggleMode: Boolean) {
    navigationIcon = ContextCompat.getDrawable(this.context, if (toggleMode) R.drawable.ic_close else R.drawable.ic_arrow_back)
}

fun MaterialToolbar.showMoreIcon(show: Boolean) {
    overflowIcon =
        if (show) {
            ContextCompat.getDrawable(this.context, R.drawable.ic_more_vert)
        } else {
            null
        }
}

fun MaterialToolbar.onMenuItemClick(callback: MenuItem.() -> Unit) {
    setOnMenuItemClickListener { menuItem ->
        callback(menuItem)
        true
    }
}

fun MaterialToolbar.updateTitle(
    vm: BaseItemsModel,
    rv: RecyclerView,
    title: Int,
    total: Int,
) {
    val bindingAdapter = rv.bindingAdapter
    this.title =
        if (bindingAdapter.toggleMode) {
            val count = bindingAdapter.getCheckedModels<Any>().size
            LocaleHelper.getStringF(R.string.x_selected, "count", count)
        } else {
            if (vm is FilteredItemsViewModel && vm.data != null) {
                val name =
                    when (vm.data) {
                        is DTag -> {
                            (vm.data as DTag).name
                        }
                        is DFeed -> {
                            (vm.data as DFeed).name
                        }
                        is DType -> {
                            getString((vm.data as DType).titleId)
                        }
                        is DMediaFolders -> {
                            getString(R.string.folders)
                        }
                        is DMediaBucket -> {
                            (vm.data as DMediaBucket).name
                        }
                        else -> {
                            ""
                        }
                    }
                LocaleHelper.getStringF(R.string.tag_title, "name", name, "count", total)
            } else {
                LocaleHelper.getStringF(title, "count", total)
            }
        }
}

fun MaterialToolbar.updateFilesTitle(
    vm: BaseItemsModel,
    rv: RecyclerView,
) {
    val bindingAdapter = rv.bindingAdapter
    this.title =
        if (bindingAdapter.toggleMode) {
            val count = bindingAdapter.getCheckedModels<Any>().size
            LocaleHelper.getStringF(R.string.x_selected, "count", count)
        } else {
            getString(R.string.files)
        }

    this.subtitle =
        if (bindingAdapter.toggleMode) {
            ""
        } else {
            val items = rv.getModelList<FileModel>()
            val foldersCount = items.count { it.data.isDir }
            val filesCount = items.count { !it.data.isDir }
            val strList = mutableListOf<String>()
            if (foldersCount > 0) {
                strList.add(LocaleHelper.getQuantityString(R.plurals.x_folders, foldersCount))
            }
            if (filesCount > 0) {
                strList.add(LocaleHelper.getQuantityString(R.plurals.x_files, filesCount))
            }
            strList.joinToString(", ")
        }
}

fun MaterialToolbar.setScrollBehavior(enabled: Boolean) {
    val flags =
        if (enabled) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            0
        }
    updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
}
