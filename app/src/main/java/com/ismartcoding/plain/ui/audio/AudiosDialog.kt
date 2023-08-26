package com.ismartcoding.plain.ui.audio

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.TagType
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.data.preference.AudioSortByPreference
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.audio.AudioAction
import com.ismartcoding.plain.features.audio.AudioHelper
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.data.enums.MediaType
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.CastDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.highlightTitle
import com.ismartcoding.plain.ui.helpers.FileSortHelper
import com.ismartcoding.plain.ui.models.DMediaFolders
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class AudiosDialog(private val bucket: DMediaBucket? = null) : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.audios_title

    override val tagType: TagType
        get() = TagType.AUDIO

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.data = bucket
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
        binding.list.checkPermission(requireContext(), Permission.WRITE_EXTERNAL_STORAGE)
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
        val context = requireContext()
        lifecycleScope.launch {
            val currentPath = withIO { AudioPlayingPreference.getValueAsync(context)?.path }
            val isAudioPlaying = AudioPlayer.instance.isPlaying()
            binding.list.rv.models?.forEach {
                if (it is AudioModel) {
                    val old = it.isPlaying
                    it.isPlaying = isAudioPlaying && currentPath == it.data.path
                    if (old != it.isPlaying) {
                        it.notifyChange()
                    }
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun updateList() {
        lifecycleScope.launch {
            if (viewModel.data is DMediaFolders) {
                updateFolders()
            } else {
                updateAudios()
            }
            updateTitle()
        }
    }

    override fun initTopAppBar() {
        lifecycleScope.launch {
            val context = requireContext()
            initTopAppBar(R.menu.media_items_top) {
                FileSortHelper.bindSortMenuItemClick(context, lifecycleScope, binding.topAppBar.toolbar.menu, this, MediaType.AUDIO, viewModel, binding.list)
            }
            val sortBy = withIO { AudioSortByPreference.getValueAsync(context) }
            FileSortHelper.getSelectedSortItem(binding.topAppBar.toolbar.menu, sortBy).highlightTitle(context)
        }

    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<AudioModel>(R.layout.item_audio)
            addType<DMediaBucket>(R.layout.item_audio_bucket)
            R.id.container.onLongClick {
                if (itemViewType == R.layout.item_audio) {
                    viewModel.toggleMode.value = true
                    rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
                }
            }

            checkable(onItemClick = {
                if (itemViewType == R.layout.item_audio_bucket) {
                    val m = getModel<DMediaBucket>()
                    AudiosDialog(m).show()
                } else {
                    val m = getModel<AudioModel>()
                    if (viewModel.castMode) {
                        CastDialog(arrayListOf(), m.data.path).show()
                    } else {
                        Permissions.checkNotification(requireContext(), R.string.audio_notification_prompt) {
                            AudioPlayerService.play(requireContext(), getModel<AudioModel>().data.toPlaylistAudio())
                        }
                    }
                }
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        initRefreshLoadMore()
    }

    override fun updateDrawerMenu() {
        if (isQPlus()) {
            updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.FOLDERS, DrawerMenuGroupType.TAGS)
        } else {
            updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
        }
    }

    private suspend fun updateAudios() {
        val query = viewModel.getQuery()
        val context = requireContext()
        val sortBy = AudioSortByPreference.getValueAsync(context)
        val items = withIO { AudioHelper.search(context, query, viewModel.limit, viewModel.offset, sortBy) }
        viewModel.total = withIO { AudioHelper.count(context, query) }

        val bindingAdapter = binding.list.rv.bindingAdapter
        val toggleMode = bindingAdapter.toggleMode
        val checkedItems = bindingAdapter.getCheckedModels<AudioModel>()
        val currentPath = withIO {  AudioPlayingPreference.getValueAsync(context)?.path  }
        val isAudioPlaying = AudioPlayer.instance.isPlaying()
        binding.list.page.addData(items.map { a ->
            AudioModel(a).apply {
                title = a.title
                subtitle = a.artist + " " + FormatHelper.formatDuration(a.duration)
                this.toggleMode = toggleMode
                isChecked = checkedItems.any { it.data.id == data.id }
                isPlaying = isAudioPlaying && currentPath == a.path
            }
        }, hasMore = {
            items.size == viewModel.limit
        })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun updateFolders() {
        val items = withIO { AudioHelper.getBuckets(requireContext()) }
        viewModel.total = items.size
        binding.list.page.addData(items, hasMore = { false })
    }
}

