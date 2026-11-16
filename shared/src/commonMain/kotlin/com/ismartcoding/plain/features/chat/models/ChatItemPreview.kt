package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.file
import com.ismartcoding.plain.i18n.files
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.images
import com.ismartcoding.plain.i18n.message
import com.ismartcoding.plain.i18n.shared_folder
import com.ismartcoding.plain.i18n.video
import com.ismartcoding.plain.i18n.videos
import com.ismartcoding.plain.ui.i18n.LocaleHelper

fun ChatItem.getMessagePreview(): String {
    val message = messageContent
    if (message.type == MessageType.SHARE) return LocaleHelper.getString(Res.string.shared_folder)
    val value = message.value as ChatMessage
    value.text?.takeIf { it.isNotBlank() }?.let { return it.take(50) }
    val items = value.images
    if (items.isNotEmpty()) {
        val videoCount = items.count { it.durationMs > 0 }
        val imageCount = items.size - videoCount
        val images = when {
            imageCount > 1 -> "$imageCount ${LocaleHelper.getString(Res.string.images)}"
            imageCount == 1 -> LocaleHelper.getString(Res.string.image)
            else -> null
        }
        val videos = when {
            videoCount > 1 -> "$videoCount ${LocaleHelper.getString(Res.string.videos)}"
            videoCount == 1 -> LocaleHelper.getString(Res.string.video)
            else -> null
        }
        return listOfNotNull(images, videos).joinToString(", ")
    }
    val count = value.files.size
    return when {
        count > 1 -> "$count ${LocaleHelper.getString(Res.string.files)}"
        count == 1 -> LocaleHelper.getString(Res.string.file)
        else -> LocaleHelper.getString(Res.string.message)
    }
}
