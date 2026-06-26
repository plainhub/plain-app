package com.ismartcoding.plain.events

import com.ismartcoding.plain.lib.channel.ChannelEvent
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.db.DChat

// Android-only events that depend on app/-side types (ChatTarget, DownloadTask).
// Pure events live in shared/commonMain/events/HttpApiEvents.kt.

class HMessageCreatedEvent(val target: ChatTarget, val items: List<DChat>) : ChannelEvent()

// Download events
class HDownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()
