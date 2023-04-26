package com.ismartcoding.plain.ui.chat.views

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.extensions.*
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ChatItemFilesBinding
import com.ismartcoding.plain.databinding.ItemChatFileBinding
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.audio.AudioPlayerService
import com.ismartcoding.plain.features.audio.DPlaylistAudio
import com.ismartcoding.plain.ui.PdfViewerDialog
import com.ismartcoding.plain.ui.TextEditorDialog
import com.ismartcoding.plain.ui.audio.AudioPlayerDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.preview.PreviewDialog
import com.ismartcoding.plain.ui.preview.PreviewItem
import com.ismartcoding.plain.ui.preview.TransitionHelper

data class FileModel(val chatItem: DChat, val id: String, val uri: String, val size: Long, val duration: Long)

fun ChatItemFilesBinding.initView() {
    val pxSM = this.rv.context.px(R.dimen.size_sm)
    val pxNormal = this.rv.context.px(R.dimen.size_normal)
    rv.linear().setup {
        addType<FileModel>(R.layout.item_chat_file)
        R.id.container.onClick {
            val m = getModel<FileModel>()
            if (m.uri.isImageFast() || m.uri.isVideoFast()) {
                PreviewDialog().show(
                    items = getMediaItems(m.chatItem).filter { it.uri.isVideoFast() || it.uri.isImageFast() }.map { s -> PreviewItem(s.id, s.uri) },
                    initKey = m.id,
                )
            } else if (m.uri.isAudioFast()) {
                AudioPlayerService.play(context, DPlaylistAudio.fromPath(context, m.uri))
                AudioPlayerDialog().show()
                Permissions.checkNotification()
            } else if (m.uri.isTextFile()) {
                if (m.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                    TextEditorDialog(m.uri).show()
                } else {
                    DialogHelper.showMessage(R.string.text_file_size_limit)
                }
            } else if (m.uri.isPdfFile()) {
                PdfViewerDialog(m.uri).show()
            }
        }
        onBind {
            val m = getModel<FileModel>()
            val b = getBinding<ItemChatFileBinding>()
            val context = b.container.context
            b.container.setSelectableItemBackground()
            val top = if (modelPosition == 0) {
                pxNormal
            } else {
                pxSM
            }
            val bottom = if (modelPosition == modelCount - 1) {
                pxNormal
            } else {
                pxSM
            }
            b.container.updatePadding(pxNormal, top, pxNormal, bottom)
            b.title.setTextColor(ContextCompat.getColor(context, R.color.primary))
            b.subtitle.setTextColor(ContextCompat.getColor(context, R.color.secondary))
            if (m.uri.isImageFast() || m.uri.isVideoFast()) {
                Glide.with(b.image).load(m.uri).placeholder(b.image.drawable).into(b.image)
                b.image.visibility = View.VISIBLE
            } else {
                b.image.visibility = View.GONE
            }
            TransitionHelper.put(m.id, b.image)
            b.title.text = m.uri.getFilenameFromPath()
            b.subtitle.text = FormatHelper.formatBytes(m.size)
        }
    }
}

private fun getMediaItems(chatItem: DChat): List<FileModel> {
    return (chatItem.content.value as DMessageFiles).items.mapIndexed { index, it -> FileModel(chatItem,chatItem.id + "|" + index, it.uri, it.size, it.duration) }
}

fun ChatItemFilesBinding.bindData(chatItem: DChat) {
    rv.models = getMediaItems(chatItem)
}