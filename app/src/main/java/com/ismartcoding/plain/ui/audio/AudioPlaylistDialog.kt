package com.ismartcoding.plain.ui.audio

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.annotaion.ItemOrientation
import com.ismartcoding.lib.brv.item.ItemDrag
import com.ismartcoding.lib.brv.listener.DefaultItemTouchCallback
import com.ismartcoding.lib.brv.utils.*
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.DialogPlaylistBinding
import com.ismartcoding.plain.features.*
import com.ismartcoding.plain.features.audio.*
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.services.AudioPlayerService
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.extensions.*

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
                        AudioPlayer.instance.pause()
                        LocalStorage.audioPlaying = null
                        LocalStorage.audioPlaylist = arrayListOf()
                        sendEvent(ClearAudioPlaylistEvent())
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
                AudioPlayerService.play(requireContext(), m.audio)
                Permissions.checkNotification()
            }

            itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {
                override fun onDrag(
                    source: BindingAdapter.BindingViewHolder,
                    target: BindingAdapter.BindingViewHolder
                ) {
                    LocalStorage.audioPlaylist = getModelList<AudioModel>().map { it.audio }
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
        binding.list.rv.getModelList<AudioModel>().forEach {
            val old = it.isPlaying
            it.checkIsPlaying(it.audio.path)
            if (old != it.isPlaying) {
                it.notifyChange()
            }
        }
    }

    private fun search() {
        binding.list.page.addData(LocalStorage.audioPlaylist
            .filter { searchQ.isEmpty() || it.title.contains(searchQ, true) || it.artist.contains(searchQ, true) }
            .map { audio ->
                AudioModel(audio).apply {
                    title = audio.title
                    subtitle = audio.artist + " " + FormatHelper.formatDuration(audio.duration)
                    checkIsPlaying(audio.path)
                    swipeEnable = true
                    rightSwipeText = getString(R.string.remove)
                    rightSwipeClick = {
                        LocalStorage.deletePlaylistAudio(audio.path)
                        binding.list.rv.apply {
                            val index = getModelList<AudioModel>().indexOfFirst { it.audio.path == audio.path }
                            if (index != -1) {
                                removeModel(index)
                            }
                        }
                        updateTitle()
                    }
                }
            })
        updateTitle()
    }

    private fun updateTitle() {
        val total = LocalStorage.audioPlaylist.size
        binding.topAppBar.title = if (total > 0) LocaleHelper.getStringF(R.string.playlist_title, "total", total) else getString(R.string.playlist)
    }
}