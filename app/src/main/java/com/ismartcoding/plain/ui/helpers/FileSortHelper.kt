package com.ismartcoding.plain.ui.helpers

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.preference.AudioSortByPreference
import com.ismartcoding.plain.preference.FileSortByPreference
import com.ismartcoding.plain.preference.ImageSortByPreference
import com.ismartcoding.plain.preference.VideoSortByPreference
import com.ismartcoding.plain.databinding.ViewPageListBinding
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.extensions.unhighlightTitle
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object FileSortHelper {
    fun getSelectedSortItem(
        menu: Menu,
        sortBy: FileSortBy,
    ): MenuItem {
        return when (sortBy) {
            FileSortBy.DATE_ASC -> {
                menu.findItem(R.id.sort_oldest_first)
            }

            FileSortBy.DATE_DESC -> {
                menu.findItem(R.id.sort_newest_first)
            }

            FileSortBy.NAME_ASC -> {
                menu.findItem(R.id.sort_name_asc)
            }

            FileSortBy.NAME_DESC -> {
                menu.findItem(R.id.sort_name_desc)
            }

            FileSortBy.SIZE_ASC -> {
                menu.findItem(R.id.sort_smallest_first)
            }

            FileSortBy.SIZE_DESC -> {
                menu.findItem(R.id.sort_largest_first)
            }
        }
    }

    private suspend fun getSortByAsync(
        context: Context,
        mediaType: MediaType,
    ): FileSortBy {
        return when (mediaType) {
            MediaType.VIDEO -> {
                VideoSortByPreference.getValueAsync(context)
            }

            MediaType.IMAGE -> {
                ImageSortByPreference.getValueAsync(context)
            }

            MediaType.AUDIO -> {
                AudioSortByPreference.getValueAsync(context)
            }

            MediaType.FILE -> {
                FileSortByPreference.getValueAsync(context)
            }
        }
    }

    private suspend fun setSortByAsync(
        context: Context,
        mediaType: MediaType,
        sortBy: FileSortBy,
    ) {
        when (mediaType) {
            MediaType.VIDEO -> {
                VideoSortByPreference.putAsync(context, sortBy)
            }

            MediaType.IMAGE -> {
                ImageSortByPreference.putAsync(context, sortBy)
            }

            MediaType.AUDIO -> {
                AudioSortByPreference.putAsync(context, sortBy)
            }

            MediaType.FILE -> {
                FileSortByPreference.putAsync(context, sortBy)
            }
        }
    }

    fun sort(
        context: Context,
        scope: CoroutineScope,
        menu: Menu,
        mediaType: MediaType,
        viewModel: FilteredItemsViewModel,
        list: ViewPageListBinding,
        sortBy: FileSortBy,
    ) {
        scope.launch {
            getSelectedSortItem(menu, withIO { getSortByAsync(context, mediaType) }).unhighlightTitle()
            withIO { setSortByAsync(context, mediaType, sortBy) }
            getSelectedSortItem(menu, withIO { getSortByAsync(context, mediaType) }).highlightTitle(context)
            viewModel.offset = 0
            list.page.refresh()
        }
    }

    fun bindSortMenuItemClick(
        context: Context,
        scope: CoroutineScope,
        menu: Menu,
        menuItem: MenuItem,
        mediaType: MediaType,
        viewModel: FilteredItemsViewModel,
        list: ViewPageListBinding,
    ) {
        when (menuItem.itemId) {
            R.id.sort_newest_first -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.DATE_DESC)
            }

            R.id.sort_oldest_first -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.DATE_ASC)
            }

            R.id.sort_largest_first -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.SIZE_DESC)
            }

            R.id.sort_smallest_first -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.SIZE_ASC)
            }

            R.id.sort_name_asc -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.NAME_ASC)
            }

            R.id.sort_name_desc -> {
                sort(context, scope, menu, mediaType, viewModel, list, FileSortBy.NAME_DESC)
            }
        }
    }
}
