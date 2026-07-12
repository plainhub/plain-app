package com.ismartcoding.plain.db

import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.file
import com.ismartcoding.plain.i18n.files
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.images
import com.ismartcoding.plain.i18n.message
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.videos

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
