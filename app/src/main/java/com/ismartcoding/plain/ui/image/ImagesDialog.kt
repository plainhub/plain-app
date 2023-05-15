package com.ismartcoding.plain.ui.image

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.rv.GridSpacingItemDecoration
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.databinding.ItemImageGridBinding
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.file.MediaType
import com.ismartcoding.plain.features.image.DImage
import com.ismartcoding.plain.features.image.ImageHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import com.ismartcoding.plain.ui.models.IDataModel
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper
import kotlinx.coroutines.launch

class ImagesDialog() : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.images_title

    override val tagType: TagType
        get() = TagType.IMAGE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        initTopAppBar(R.menu.media_items_top) {
            FileSortHelper.bindSortMenuItemClick(requireContext(), binding.topAppBar.toolbar.menu, this, MediaType.IMAGE, viewModel, binding.list)
        }
        FileSortHelper.getSelectedSortItem(binding.topAppBar.toolbar.menu, LocalStorage.imageSortBy).highlightTitle(requireContext())
    }

    override fun initList() {
        val spanCount = 3
        val context = requireContext()
        val rv = binding.list.rv
        rv.layoutManager = GridLayoutManager(context, spanCount)
        rv.setup {
            addType<ImageModel>(R.layout.item_image_grid)
            onBind {
                val m = getModel<ImageModel>()
                val b = getBinding<ItemImageGridBinding>()
                TransitionHelper.put(m.data.id, b.image)
            }

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                val m = getModel<ImageModel>()
                if (viewModel.castMode) {
                    CastDialog(arrayListOf(), m.data.path).show()
                } else {
                    PreviewDialog().show(
                        items = getModelList<ImageModel>().map { s -> PreviewItem(s.data.id, s.data.path) },
                        initKey = m.data.id,
                    )
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
        binding.list.checkPermission(Permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { ImageHelper.search(requireContext(), query, viewModel.limit, viewModel.offset, LocalStorage.imageSortBy) }
            viewModel.total = withIO { ImageHelper.count(requireContext(), query) }

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
            updateTitle()
        }
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
    }
}

