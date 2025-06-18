package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState

// The events sent from the HTTP API
class HttpApiEvents {
    class MessageCreatedEvent(val items: List<DChat>) : ChannelEvent()

    class MessageUpdatedEvent(val id: String) : ChannelEvent()
    
    // Pomodoro events
    class PomodoroStartEvent(val timeLeft: Int) : ChannelEvent()
    
    class PomodoroPauseEvent : ChannelEvent()
    
    class PomodoroStopEvent : ChannelEvent()
}