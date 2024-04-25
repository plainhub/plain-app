package com.ismartcoding.plain.ui.image

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
import com.ismartcoding.plain.databinding.DialogListDrawerBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.image.DImage
import com.ismartcoding.plain.features.image.ImageMediaStoreHelper
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilteredItemsViewModel
import kotlinx.coroutines.launch

object ImagesBottomMenuHelper {
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
                    ShareHelper.shareUris(context, ArrayList(items.map { ImageMediaStoreHelper.getItemUri(it.data.id) }))
                }
            }
            R.id.cast -> {
                rv.ensureSelect { items ->
                    CastDialog(arrayListOf(), (items[0].data as DImage).path).show()
                }
            }
            R.id.delete -> {
                rv.ensureSelect { items ->
                    DialogHelper.confirmToDelete {
                        lifecycleScope.launch {
                            val ids = items.map { it.data.id }.toSet()
                            DialogHelper.showLoading()
                            withIO {
                                TagHelper.deleteTagRelationByKeys(ids, viewModel.dataType)
                                ImageMediaStoreHelper.deleteRecordsAndFilesByIds(context, ids)
                            }
                            DialogHelper.hideLoading()
                            rv.bindingAdapter.checkedAll(false)
                            sendEvent(ActionEvent(ActionSourceType.IMAGE, ActionType.DELETED, ids))
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
