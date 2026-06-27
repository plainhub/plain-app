package com.ismartcoding.plain.events

import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.channel.ChannelEvent

data class HDownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()

class HMessageCreatedEvent(val target: ChatTarget, val items: List<DChat>) : ChannelEvent()
