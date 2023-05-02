package com.ismartcoding.plain.ui.audio

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.getModelList
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.audio.AudioAction
import com.ismartcoding.plain.features.audio.AudioHelper
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.features.file.MediaType
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class AudiosDialog() : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.audios_title

    override val tagType: TagType
        get() = TagType.AUDIO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.rv.setPadding(0, 0, 0, requireContext().dp2px(72))
        binding.player.isVisible = true
        binding.player.initView()
        checkPermission()
        initBottomBar(R.menu.action_audios) {
            AudiosBottomMenuHelper.onMenuItemClick(requireContext(), viewModel, lifecycleScope, binding, this)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.player.updateUI()
        updatePlayingState()
    }

    private fun checkPermission() {
        binding.list.checkPermission(Permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun initEvents() {
        receiveEvent<PermissionResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.AUDIO) {
                binding.list.page.refresh()
            }
        }
        receiveEvent<AudioActionEvent> { event ->
            when (event.action) {
                AudioAction.PLAY, AudioAction.PAUSE -> {
                    updatePlayingState()
                }
                else -> {}
            }
        }
        receiveEvent<ClearAudioPlaylistEvent> {
            binding.player.updateUI()
        }
    }

    private fun updatePlayingState() {
        binding.list.rv.getModelList<AudioModel>().forEach {
            val old = it.isPlaying
            it.checkIsPlaying(it.data.path)
            if (old != it.isPlaying) {
                it.notifyChange()
            }
        }
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { AudioHelper.search(requireContext(), query, viewModel.limit, viewModel.offset, LocalStorage.audioSortBy) }
            viewModel.total = withIO { AudioHelper.count(requireContext(), query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<AudioModel>()
            binding.list.page.addData(items.map { a ->
                AudioModel(a).apply {
                    title = a.title
                    subtitle = a.artist + " " + FormatHelper.formatDuration(a.duration)
                    this.toggleMode = toggleMode
                    isChecked = checkedItems.any { it.data.id == data.id }
                    checkIsPlaying(a.path)
                }
            }, hasMore = {
                items.size == viewModel.limit
            })
            updateTitle()
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.media_items_top) {
            FileSortHelper.bindSortMenuItemClick(requireContext(), binding.topAppBar.toolbar.menu, this, MediaType.AUDIO, viewModel, binding.list)
        }
        FileSortHelper.getSelectedSortItem(binding.topAppBar.toolbar.menu, LocalStorage.audioSortBy).highlightTitle(requireContext())
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<AudioModel>(R.layout.item_audio)
            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                AudioPlayerService.play(requireContext(), getModel<AudioModel>().data.toPlaylistAudio())
                Permissions.checkNotification()
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        initRefreshLoadMore()
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
    }
}

