package com.ismartcoding.plain.ui.chat.views

import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.rv.GridSpacingItemDecoration
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ChatItemImagesBinding
import com.ismartcoding.plain.databinding.ViewGridImageBinding
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.lib.extensions.glide
import com.ismartcoding.plain.features.ChatItemClickEvent
import com.ismartcoding.plain.ui.extensions.setSafeClick


data class ImageModel(val chatItem: DChat, val id: String, val uri: String, val size: Long, val duration: Long)

fun ChatItemImagesBinding.initView() {
    val context = rv.context
    val spanCount = 3
    rv.layoutManager = GridLayoutManager(context, spanCount)
    rv.setup {
        addType<ImageModel>(R.layout.view_grid_image)
        onBind {
            val m = getModel<ImageModel>()
            val b = getBinding<ViewGridImageBinding>()
            b.image.glide(m.uri)
            TransitionHelper.put(m.id, b.image)
            b.image.setSafeClick {
                sendEvent(ChatItemClickEvent())
                PreviewDialog().show(
                    items = getMediaItems(m.chatItem).map { s -> PreviewItem(s.id, s.uri) },
                    initKey = m.id,
                )
            }
            b.duration.run {
                if (m.duration > 0) {
                    text = FormatHelper.formatDuration(m.duration)
                    isVisible = true
                } else {
                    isVisible = false
                }
            }
        }
    }
    rv.addItemDecoration(GridSpacingItemDecoration(spanCount, context.dp2px(1).toInt(), false))
    rv.setHasFixedSize(true)
}

private fun getMediaItems(chatItem: DChat): List<ImageModel> {
    return (chatItem.content.value as DMessageImages).items.mapIndexed { index, it -> ImageModel(chatItem, chatItem.id + "|" + index, it.uri, it.size, it.duration) }
}

fun ChatItemImagesBinding.bindData(chatItem: DChat) {
    rv.models = getMediaItems(chatItem)
}