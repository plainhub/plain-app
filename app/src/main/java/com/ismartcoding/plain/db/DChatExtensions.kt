package com.ismartcoding.plain.db

import android.content.Context
import com.ismartcoding.plain.lib.extensions.getFinalPath
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.file
import com.ismartcoding.plain.i18n.files
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.images
import com.ismartcoding.plain.i18n.message
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.videos

fun DMessageFile.getPreviewPath(context: Context, peer: DPeer?): String {
    return if (isRemoteFile()) {
        peer?.getFileUrl(parseFileId()) + "&w=200&h=200"
    } else {
        uri.getFinalPath(context)
    }
}

fun DMessageContent.toPeerMessageContent(): DMessageContent {
    return when (type) {
        DMessageType.FILES.value -> {
            val files = value as DMessageFiles
            val modified = files.items.map { file ->
                val fileId = FileHelper.getFileId(file.uri)
                file.copy(uri = "fsid:$fileId")
            }
            DMessageContent(type, DMessageFiles(modified))
        }

        DMessageType.IMAGES.value -> {
            val images = value as DMessageImages
            val modified = images.items.map { image ->
                val fileId = FileHelper.getFileId(image.uri)
                image.copy(uri = "fsid:$fileId")
            }
            DMessageContent(type, DMessageImages(modified))
        }

        else -> this
    }
}

fun DChat.getMessagePreview(): String {
    return when (content.type) {
        DMessageType.TEXT.value -> {
            val textMessage = content.value as? DMessageText
            textMessage?.text?.take(50) ?: LocaleHelper.getString(Res.string.message)
        }

        DMessageType.IMAGES.value -> {
            val imagesMessage = content.value as? DMessageImages
            val items = imagesMessage?.items ?: emptyList()
            val videoCount = items.count { it.duration > 0 }
            val imageCount = items.size - videoCount
            when {
                imageCount > 0 && videoCount > 0 -> {
                    val imgPart = if (imageCount > 1) "$imageCount ${LocaleHelper.getString(Res.string.images)}" else LocaleHelper.getString(Res.string.image)
                    val vidPart = if (videoCount > 1) "$videoCount ${LocaleHelper.getString(Res.string.videos)}" else LocaleHelper.getString(Res.string.video)
                    "$imgPart, $vidPart"
                }

                videoCount > 0 -> {
                    if (videoCount > 1) "$videoCount ${LocaleHelper.getString(Res.string.videos)}" else LocaleHelper.getString(Res.string.video)
                }

                else -> {
                    if (imageCount > 1) "$imageCount ${LocaleHelper.getString(Res.string.images)}" else LocaleHelper.getString(Res.string.image)
                }
            }
        }

        DMessageType.FILES.value -> {
            val filesMessage = content.value as? DMessageFiles
            val count = filesMessage?.items?.size ?: 0
            if (count > 1) "$count ${LocaleHelper.getString(Res.string.files)}" else LocaleHelper.getString(Res.string.file)
        }

        else -> LocaleHelper.getString(Res.string.message)
    }
}
