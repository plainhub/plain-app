package com.ismartcoding.plain.ui.audio

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaType
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.preference.AudioSortByPreference
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.features.ClearAudioPlaylistEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.features.AudioPlayer
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

    override val dataType: DataType
        get() = DataType.AUDIO

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
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
        binding.list.checkPermission(requireContext(), AppFeatureType.FILES)
    }

    override fun initEvents() {
        receiveEvent<PermissionsResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.AUDIO) {
                binding.list.page.refresh()
            }
        }
        receiveEvent<AudioActionEvent> { event ->
            when (event.action) {
                AudioAction.PLAYBACK_STATE_CHANGED, AudioAction.MEDIA_ITEM_TRANSITION -> {
                    updatePlayingState()
                }

                else -> {}
            }
        }
        receiveEvent<ClearAudioPlaylistEvent> {
            binding.player.updateUI()
            updatePlayingState()
        }
    }

    private fun updatePlayingState() {
        val context = requireContext()
        lifecycleScope.launch {
            val currentPath = withIO { AudioPlayingPreference.getValueAsync(context) }
            val isAudioPlaying = AudioPlayer.isPlaying()
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
                FileSortHelper.bindSortMenuItemClick(
                    context,
                    lifecycleScope,
                    binding.topAppBar.toolbar.menu,
                    this,
                    MediaType.AUDIO,
                    viewModel,
                    binding.list,
                )
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
                        val context = requireContext()
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            AudioPlayer.play(context, m.data.toPlaylistAudio())
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
        val items = withIO { AudioMediaStoreHelper.searchAsync(context, query, viewModel.limit, viewModel.offset, sortBy) }
        viewModel.total = withIO { AudioMediaStoreHelper.countAsync(context, query) }
        val bindingAdapter = binding.list.rv.bindingAdapter
        val toggleMode = bindingAdapter.toggleMode
        val checkedItems = bindingAdapter.getCheckedModels<AudioModel>()
        val currentPath = withIO { AudioPlayingPreference.getValueAsync(context) }
        val isAudioPlaying = AudioPlayer.isPlaying()
        binding.list.page.addData(
            items.map { a ->
                AudioModel(a).apply {
                    title = a.title
                    subtitle = a.artist + " " + a.duration.formatDuration()
                    this.toggleMode = toggleMode
                    isChecked = checkedItems.any { it.data.id == data.id }
                    isPlaying = isAudioPlaying && currentPath == a.path
                }
            },
            hasMore = {
                items.size == viewModel.limit
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun updateFolders() {
        val items = withIO { AudioMediaStoreHelper.getBucketsAsync(requireContext()) }
        viewModel.total = items.size
        binding.list.page.addData(items, hasMore = { false })
    }
}
