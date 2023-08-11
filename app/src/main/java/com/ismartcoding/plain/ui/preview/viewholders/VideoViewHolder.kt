package com.ismartcoding.plain.ui.preview.viewholders

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.media.VideoModel
import com.ismartcoding.lib.media.VideoPlayer
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.databinding.ItemImageviewerVideoBinding
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.ViewerShowCastListEvent
import com.ismartcoding.plain.ui.preview.utils.initTag
import java.io.File

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
        binding.imageView.load(item.uri)
        binding.videoView.binding.title.text = _item.uri.getFilenameFromPath()
        binding.videoView.binding.ivCast.setSafeClick {
            sendEvent(ViewerShowCastListEvent(item.uri))
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
            player.setMediaSource(MainApp.instance, VideoModel(Uri.fromFile(File(_item.uri))))
            binding.videoView.bindMediaPlayer(player)
            binding.videoView.play()
        }
    }
}
