package com.ismartcoding.plain.events

import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.enums.WebSettingsFeature
import com.ismartcoding.plain.lib.ChannelEvent

// Pomodoro events
class HPomodoroStartEvent(val timeLeft: Int) : ChannelEvent()

class HPomodoroPauseEvent : ChannelEvent()

class HPomodoroStopEvent : ChannelEvent()

class HStartScreenMirrorEvent(val audio: Boolean) : ChannelEvent()

class HRequestScreenMirrorAudioEvent : ChannelEvent()

class HOpenAccessibilitySettingsEvent : ChannelEvent()

class HOpenWebSettingsEvent(val feature: WebSettingsFeature? = null) : ChannelEvent()

class HEnableImageSearchEvent : ChannelEvent()
class HDisableImageSearchEvent : ChannelEvent()
class HCancelImageModelDownloadEvent : ChannelEvent()

class HCancelNotificationsEvent(val ids: Set<String>) : ChannelEvent()

data class HDownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()
