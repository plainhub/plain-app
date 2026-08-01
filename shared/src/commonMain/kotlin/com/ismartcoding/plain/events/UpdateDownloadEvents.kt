package com.ismartcoding.plain.events

import com.ismartcoding.plain.lib.ChannelEvent

class UpdateDownloadProgressEvent(val progress: Int) : ChannelEvent()
class UpdateDownloadCompleteEvent(val filePath: String) : ChannelEvent()
class UpdateDownloadFailedEvent : ChannelEvent()
