package com.ismartcoding.plain.ui.image

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.lib.helpers.BitmapHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.rv.GridSpacingItemDecoration
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.data.preference.ImageSortByPreference
import com.ismartcoding.plain.databinding.ItemImageGridBinding
import com.ismartcoding.plain.databinding.ItemMediaBucketGridBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.data.enums.MediaType
import com.ismartcoding.plain.features.image.ImageHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import com.ismartcoding.plain.ui.models.DMediaFolders
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper
import com.ismartcoding.plain.ui.views.mergeimages.CombineBitmapTools
import kotlinx.coroutines.launch

class ImagesDialog(val bucket: DMediaBucket? = null) : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.images_title

    override val tagType: TagType
        get() = TagType.IMAGE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.data = bucket
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        initBottomBar(R.menu.action_images) {
            ImagesBottomMenuHelper.onMenuItemClick(requireContext(), viewModel, lifecycleScope, binding, this)
        }
    }

    override fun initEvents() {
        receiveEvent<PermissionResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.IMAGE) {
                binding.list.page.refresh()
            }
        }
    }

    override fun initTopAppBar() {
        lifecycleScope.launch {
            val context = requireContext()
            initTopAppBar(R.menu.media_items_top) {
                FileSortHelper.bindSortMenuItemClick(context, lifecycleScope, binding.topAppBar.toolbar.menu, this, MediaType.IMAGE, viewModel, binding.list)
            }
            FileSortHelper.getSelectedSortItem(binding.topAppBar.toolbar.menu, withIO { ImageSortByPreference.getValueAsync(context) }).highlightTitle(context)
        }
    }


    override fun initList() {
        val spanCount = 3
        val context = requireContext()
        val rv = binding.list.rv
        rv.layoutManager = GridLayoutManager(context, spanCount)
        rv.setup {
            addType<ImageModel>(R.layout.item_image_grid)
            addType<DMediaBucket>(R.layout.item_media_bucket_grid)
            onBind {
                if (itemViewType == R.layout.item_media_bucket_grid) {
                    val m = getModel<DMediaBucket>()
                    val b = getBinding<ItemMediaBucketGridBinding>()
                    coMain {
                        val bitmaps = withIO {
                            val bms = mutableListOf<Bitmap>()
                            m.topItems.forEach { path ->
                                val bm = BitmapHelper.decodeBitmapFromFileAsync(context, path, 200, 200)
                                if (bm != null) {
                                    bms.add(bm)
                                }
                            }
                            bms
                        }
                        try {
                            b.image.setImageBitmap(
                                CombineBitmapTools.combineBitmap(
                                    200, 200,
                                    bitmaps
                                )
                            )
                        } catch (ex: Exception) {
                            LogCat.e(ex.toString())
                        }
                    }
                } else {
                    val m = getModel<ImageModel>()
                    val b = getBinding<ItemImageGridBinding>()
                    TransitionHelper.put(m.data.id, b.image)
                }
            }

            R.id.container.onLongClick {
                if (itemViewType == R.layout.item_image_grid) {
                    viewModel.toggleMode.value = true
                    rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
                }
            }

            checkable(onItemClick = {
                if (itemViewType == R.layout.item_media_bucket_grid) {
                    val m = getModel<DMediaBucket>()
                    ImagesDialog(m).show()
                } else {
                    val m = getModel<ImageModel>()
                    if (viewModel.castMode) {
                        CastDialog(arrayListOf(), m.data.path).show()
                    } else {
                        PreviewDialog().show(
                            items = getModelList<ImageModel>().map { s -> PreviewItem(s.data.id, s.data.path.pathToUri()) },
                            initKey = m.data.id,
                        )
                    }
                }
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }
        rv.addItemDecoration(GridSpacingItemDecoration(spanCount, context.dp2px(1), false))
        rv.setHasFixedSize(true)
        initRefreshLoadMore()
    }

    private fun checkPermission() {
        binding.list.checkPermission(requireContext(), Permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun updateList() {
        lifecycleScope.launch {
            if (viewModel.data is DMediaFolders) {
                updateFolders()
            } else {
                updateImages()
            }
            updateTitle()
        }
    }

    private suspend fun updateImages() {
        val query = viewModel.getQuery()
        val context = requireContext()
        val items = withIO { ImageHelper.search(context, query, viewModel.limit, viewModel.offset, ImageSortByPreference.getValueAsync(context)) }
        viewModel.total = withIO { ImageHelper.count(context, query) }

        val bindingAdapter = binding.list.rv.bindingAdapter
        val toggleMode = bindingAdapter.toggleMode
        val checkedItems = bindingAdapter.getCheckedModels<ImageModel>()
        binding.list.page.addData(items.map { a ->
            ImageModel(a).apply {
                title = a.title
                this.toggleMode = toggleMode
                size = FormatHelper.formatBytes(a.size)
                isChecked = checkedItems.any { it.data.id == data.id }
            }
        }, hasMore = {
            items.size == viewModel.limit
        })
    }

    private suspend fun updateFolders() {
        val items = withIO { ImageHelper.getBuckets(requireContext()) }
        viewModel.total = items.size
        binding.list.page.addData(items, hasMore = { false })
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.FOLDERS, DrawerMenuGroupType.TAGS)
    }

}

