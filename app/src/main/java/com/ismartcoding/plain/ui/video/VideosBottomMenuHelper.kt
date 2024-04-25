package com.ismartcoding.plain.ui.video

import android.content.Context
import android.view.MenuItem
import androidx.lifecycle.LifecycleCoroutineScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.preference.VideoPlaylistPreference
import com.ismartcoding.plain.databinding.DialogListDrawerBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.features.video.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import kotlinx.coroutines.launch

object VideosBottomMenuHelper {
    fun onMenuItemClick(
        context: Context,
        viewModel: FilteredItemsViewModel,
        lifecycleScope: LifecycleCoroutineScope,
        binding: DialogListDrawerBinding,
        menuItem: MenuItem,
    ) {
        val list = binding.list
        val rv = list.rv
        when (menuItem.itemId) {
            R.id.share -> {
                rv.ensureSelect { items ->
                    ShareHelper.shareUris(context, ArrayList(items.map { VideoMediaStoreHelper.getItemUri(it.data.id) }))
                }
            }

            R.id.cast -> {
                rv.ensureSelect { items ->
                    CastDialog(items.map { it.data as DVideo }).show()
                }
            }

            R.id.add_to_playlist -> {
                rv.ensureSelect { items ->
                    lifecycleScope.launch {
                        withIO { VideoPlaylistPreference.addAsync(context, items.map { it.data as DVideo }) }
                        rv.bindingAdapter.checkedAll(false)
                        DialogHelper.showMessage(R.string.added_to_playlist)
                    }
                }
            }

            R.id.delete -> {
                rv.ensureSelect { items ->
                    DialogHelper.confirmToDelete {
                        lifecycleScope.launch {
                            val ids = items.map { it.data.id }.toSet()
                            withIO {
                                TagHelper.deleteTagRelationByKeys(
                                    ids,
                                    DataType.VIDEO,
                                )
                                VideoMediaStoreHelper.deleteRecordsAndFilesByIds(
                                    context,
                                    ids,
                                )
                                VideoPlaylistPreference.deleteAsync(context, ids.map { VideoMediaStoreHelper.getItemUri(it).toString() }.toSet())
                            }
                            list.rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.VIDEO, ActionType.DELETED, ids))
                        }
                    }
                }
            }

            else -> {
                BottomMenuHelper.onMenuItemClick(viewModel, binding, menuItem)
            }
        }
    }
}
