package com.ismartcoding.plain.ui.audio

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.annotaion.ItemOrientation
import com.ismartcoding.lib.brv.item.ItemDrag
import com.ismartcoding.lib.brv.listener.DefaultItemTouchCallback
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.preference.AudioPlayingPreference
import com.ismartcoding.plain.data.preference.AudioPlaylistPreference
import com.ismartcoding.plain.databinding.DialogPlaylistBinding
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioPlaylistDialog : BaseBottomSheetDialog<DialogPlaylistBinding>() {
    data class AudioModel(val audio: DPlaylistAudio, override var itemOrientationDrag: Int = ItemOrientation.ALL) : BaseAudioModel(), ItemDrag

    private var searchQ: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
                                AudioPlayer.instance.pause()
                                AudioPlayingPreference.putAsync(context, null)
                                AudioPlaylistPreference.putAsync(context, arrayListOf())
                                sendEvent(ClearAudioPlaylistEvent())
                            }
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
                    AudioPlayerService.play(requireContext(), m.audio)
                }
            }

            itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {
                override fun onDrag(
                    source: BindingAdapter.BindingViewHolder,
                    target: BindingAdapter.BindingViewHolder
                ) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        AudioPlaylistPreference.putAsync(requireContext(), getModelList<AudioModel>().map { it.audio })
                    }
                }
            })
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
            if (setOf(AudioAction.PLAY, AudioAction.PAUSE).contains(event.action)) {
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
        binding.list.rv.getModelList<AudioModel>().forEach {
            val old = it.isPlaying
            it.checkIsPlaying(context, it.audio.path)
            if (old != it.isPlaying) {
                it.notifyChange()
            }
        }
    }

    private fun search() {
        val context = requireContext()
        binding.list.page.addData(AudioPlaylistPreference.getValue(context)
            .filter { searchQ.isEmpty() || it.title.contains(searchQ, true) || it.artist.contains(searchQ, true) }
            .map { audio ->
                AudioModel(audio).apply {
                    title = audio.title
                    subtitle = audio.artist + " " + FormatHelper.formatDuration(audio.duration)
                    checkIsPlaying(context, audio.path)
                    swipeEnable = true
                    rightSwipeText = getString(R.string.remove)
                    rightSwipeClick = {
                        lifecycleScope.launch {
                            val context = requireContext()
                            withIO { AudioPlaylistPreference.deleteAsync(context, setOf(audio.path)) }
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
            })
        updateTitle()
    }

    private fun updateTitle() {
        val total = AudioPlaylistPreference.getValue(requireContext()).size
        binding.topAppBar.title = if (total > 0) LocaleHelper.getStringF(R.string.playlist_title, "total", total) else getString(R.string.playlist)
    }
}