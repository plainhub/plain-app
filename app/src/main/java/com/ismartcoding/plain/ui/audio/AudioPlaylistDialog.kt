package com.ismartcoding.plain.ui.audio

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.annotaion.ItemOrientation
import com.ismartcoding.lib.brv.item.ItemDrag
import com.ismartcoding.lib.brv.listener.DefaultItemTouchCallback
import com.ismartcoding.lib.brv.utils.getModelList
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.removeModel
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.preference.AudioPlaylistPreference
import com.ismartcoding.plain.databinding.DialogPlaylistBinding
import com.ismartcoding.plain.features.AudioActionEvent
import com.ismartcoding.plain.features.ClearAudioPlaylistEvent
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.features.audio.AudioPlayer
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FormatHelper
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.initMenu
import com.ismartcoding.plain.ui.extensions.onMenuItemClick
import com.ismartcoding.plain.ui.extensions.onSearch
import kotlinx.coroutines.launch

class AudioPlaylistDialog : BaseBottomSheetDialog<DialogPlaylistBinding>() {
    data class AudioModel(
        val audio: DPlaylistAudio,
        override var itemOrientationDrag: Int = ItemOrientation.ALL,
    ) : BaseAudioModel(), ItemDrag

    private var searchQ: String = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        initEvents()

        binding.topAppBar.run {
            initMenu(R.menu.playlist, overflow = true)
            menu.findItem(R.id.cast).isVisible = false
            onMenuItemClick {
                when (itemId) {
                    R.id.clear_list -> {
                        lifecycleScope.launch {
                            val context = requireContext()
                            withIO {
                                AudioPlayingPreference.putAsync(context, "")
                                AudioPlaylistPreference.putAsync(context, arrayListOf())
                            }
                            AudioPlayer.clear()
                            sendEvent(ClearAudioPlaylistEvent())
                        }
                    }
                }
            }

            onSearch { q ->
                if (searchQ != q) {
                    searchQ = q
                    binding.list.page.refresh()
                }
            }
        }

        binding.list.rv.linear().setup {
            addType<AudioModel>(R.layout.item_audio)
            R.id.container.onClick {
                val m = getModel<AudioModel>()
                Permissions.checkNotification(requireContext(), R.string.audio_notification_prompt) {
                    AudioPlayer.justPlay(requireContext(), m.audio)
                }
            }

            itemTouchHelper =
                ItemTouchHelper(
                    object : DefaultItemTouchCallback() {
                        override fun onDrag(
                            source: BindingAdapter.BindingViewHolder,
                            target: BindingAdapter.BindingViewHolder,
                        ) {
                            lifecycleScope.launch {
                                val audios = getModelList<AudioModel>().map { it.audio }
                                withIO { AudioPlaylistPreference.putAsync(requireContext(), audios) }
                            }
                        }
                    },
                )
        }

        binding.list.page.run {
            setEnableRefresh(false)
            setEnableNestedScroll(false)
            onRefresh {
                search()
            }
        }
    }

    private fun initEvents() {
        receiveEvent<AudioActionEvent> { event ->
            if (event.action == AudioAction.PLAYBACK_STATE_CHANGED) {
                updatePlayingState()
            }
        }

        receiveEvent<ClearAudioPlaylistEvent> {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.list.page.showLoading()
    }

    private fun updatePlayingState() {
        val context = requireContext()
        lifecycleScope.launch {
            val currentPath = withIO { AudioPlayingPreference.getValueAsync(context) }
            val isAudioPlaying = AudioPlayer.isPlaying()
            binding.list.rv.getModelList<AudioModel>().forEach {
                val old = it.isPlaying
                it.isPlaying = isAudioPlaying && currentPath == it.audio.path
                if (old != it.isPlaying) {
                    it.notifyChange()
                }
            }
        }
    }

    private fun search() {
        lifecycleScope.launch {
            val context = requireContext()
            val audios =
                withIO {
                    AudioPlaylistPreference.getValueAsync(context)
                        .filter {
                            searchQ.isEmpty() ||
                                    it.title.contains(searchQ, true) ||
                                    it.artist.contains(searchQ, true)
                        }
                }
            val currentPath = withIO { AudioPlayingPreference.getValueAsync(context) }
            val isAudioPlaying = AudioPlayer.isPlaying()
            binding.list.page.addData(
                audios
                    .map { audio ->
                        AudioModel(audio).apply {
                            title = audio.title
                            subtitle = audio.artist + " " + audio.duration.formatDuration()
                            isPlaying = isAudioPlaying && currentPath == audio.path
                            swipeEnable = true
                            rightSwipeText = getString(R.string.remove)
                            rightSwipeClick = {
                                lifecycleScope.launch {
                                    withIO { AudioPlaylistPreference.deleteAsync(requireContext(), setOf(audio.path)) }
                                    binding.list.rv.apply {
                                        val index = getModelList<AudioModel>().indexOfFirst { it.audio.path == audio.path }
                                        if (index != -1) {
                                            removeModel(index)
                                        }
                                    }
                                    updateTitle()
                                }
                            }
                        }
                    },
            )
            updateTitle()
        }
    }

    private fun updateTitle() {
        lifecycleScope.launch {
            val total = withIO { AudioPlaylistPreference.getValueAsync(requireContext()).size }
            binding.topAppBar.title = if (total > 0) LocaleHelper.getStringF(R.string.playlist_title, "total", total) else getString(R.string.playlist)
        }
    }
}
