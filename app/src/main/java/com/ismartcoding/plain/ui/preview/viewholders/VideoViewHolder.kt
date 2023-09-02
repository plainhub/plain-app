package com.ismartcoding.plain.ui.preview.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getFileName
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.media.VideoModel
import com.ismartcoding.lib.media.VideoPlayer
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.databinding.ItemImageviewerVideoBinding
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.ViewerShowCastListEvent
import com.ismartcoding.plain.ui.preview.utils.initTag

class VideoViewHolder(
    parent: ViewGroup,
    val binding: ItemImageviewerVideoBinding =
        ItemImageviewerVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var _item: PreviewItem
    private val player = VideoPlayer()

    fun bind(item: PreviewItem) {
        _item = item
        binding.videoView.initTag(item, this)
        binding.imageView.visibility = View.GONE
        Glide.with(binding.imageView).load(item.uri)
            .placeholder(binding.imageView.drawable)
            .into(binding.imageView)
        binding.videoView.binding.title.text = _item.uri.getFileName(MainApp.instance)
        binding.videoView.binding.ivCast.setSafeClick {
            sendEvent(ViewerShowCastListEvent(item.uri.toString()))
        }
    }

    fun release() {
        binding.videoView.release()
    }

    fun pause() {
        player.release()
    }

    fun resume() {
        // surrounded with coMain to fix the bug that it only shows loading in some case.
        coMain {
            player.setMediaSource(MainApp.instance, VideoModel(_item.uri))
            binding.videoView.bindMediaPlayer(player)
            binding.videoView.play()
        }
    }
}
