package com.ismartcoding.plain.ui.video

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.databinding.ItemVideoGridBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionResultEvent
import com.ismartcoding.plain.features.file.MediaType
import com.ismartcoding.plain.features.video.VideoHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import com.ismartcoding.plain.ui.image.ImageModel
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper
import kotlinx.coroutines.launch

class VideosDialog() : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.videos_title

    override val tagType: TagType
        get() = TagType.VIDEO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initFab()
        checkPermission()
        initBottomBar(R.menu.action_videos) {
            VideosBottomMenuHelper.onMenuItemClick(requireContext(), viewModel, lifecycleScope, binding, this)
        }
    }

    override fun initEvents() {
        receiveEvent<PermissionResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.VIDEO) {
                binding.list.page.refresh()
            }
        }
    }

    private fun checkPermission() {
        binding.list.checkPermission(Permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
    }

    override fun initList() {
        val spanCount = 3
        val context = requireContext()
        val rv = binding.list.rv
        rv.layoutManager = GridLayoutManager(context, spanCount)
        rv.setup {
            addType<VideoModel>(R.layout.item_video_grid)
            onBind {
                val m = getModel<VideoModel>()
                val b = getBinding<ItemVideoGridBinding>()
                TransitionHelper.put(m.data.id, b.image)
            }

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                val m = getModel<VideoModel>()
                if (viewModel.castMode) {
                    CastDialog(arrayListOf(), m.data.path).show()
                } else {
                    PreviewDialog().show(
                        items = getModelList<VideoModel>().map { s -> PreviewItem(s.data.id, s.data.path) },
                        initKey = getModel<VideoModel>().data.id,
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

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { VideoHelper.search(requireContext(), query, viewModel.limit, viewModel.offset, LocalStorage.videoSortBy) }
            viewModel.total = withIO { VideoHelper.count(requireContext(), query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<VideoModel>()
            binding.list.page.addData(items.map { a ->
                VideoModel(a).apply {
                    title = a.title
                    this.toggleMode = toggleMode
                    duration = FormatHelper.formatDuration(a.duration)
                    isChecked = checkedItems.any { it.data.id == data.id }
                }
            }, hasMore = {
                items.size == viewModel.limit
            })
            updateTitle()
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.media_items_top) {
            FileSortHelper.bindSortMenuItemClick(requireContext(), binding.topAppBar.toolbar.menu, this, MediaType.VIDEO, viewModel, binding.list)
        }
        FileSortHelper.getSelectedSortItem(binding.topAppBar.toolbar.menu, LocalStorage.videoSortBy).highlightTitle(requireContext())
    }

    private fun initFab() {
        binding.fab.run {
            isVisible = true
            setImageResource(R.drawable.ic_subscriptions)
            setSafeClick {
                VideoPlaylistDialog().show()
            }
        }
    }
}

