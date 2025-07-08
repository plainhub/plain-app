package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.chat.DownloadTask
import com.ismartcoding.plain.db.DChat

// The events sent from the HTTP API
class HttpApiEvents {
    class MessageCreatedEvent(val items: List<DChat>) : ChannelEvent()

    class MessageUpdatedEvent(val id: String) : ChannelEvent()
    
    // Download events
    class DownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()
    
    // Pomodoro events
    class PomodoroStartEvent(val timeLeft: Int) : ChannelEvent()
    
    class PomodoroPauseEvent : ChannelEvent()
    
    class PomodoroStopEvent : ChannelEvent()
}