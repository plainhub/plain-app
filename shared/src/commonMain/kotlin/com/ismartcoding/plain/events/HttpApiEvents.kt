package com.ismartcoding.plain.events

import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.ChannelEvent

class HMessageCreatedEvent(val target: ChatTarget, val items: List<DChat>) : ChannelEvent()

class HMessageUpdatedEvent(val id: String) : ChannelEvent()

// Pomodoro events
class HPomodoroStartEvent(val timeLeft: Int) : ChannelEvent()

class HPomodoroPauseEvent : ChannelEvent()

class HPomodoroStopEvent : ChannelEvent()

class HStartScreenMirrorEvent(val audio: Boolean) : ChannelEvent()

class HRequestScreenMirrorAudioEvent : ChannelEvent()

class HOpenAccessibilitySettingsEvent : ChannelEvent()

class HOpenWebSettingsEvent : ChannelEvent()

class HRetryChatItemEvent(val item: DChat) : ChannelEvent()
/**
 * Fired after the default SMS app is launched for an MMS send.
 * AppEvents polls content://mms for the correlated row and always clears
 * temporary state, emitting MMS_SENT on success or MMS_SEND_RESULT on timeout.
 */
data class HStartMmsPollingEvent(
    val pendingId: String,
    val launchTimeSec: Long,
    val minimumMmsId: Long,
    val number: String,
    val body: String,
    val threadId: String,
    val attachmentPaths: List<String>,
    val attachmentContentTypes: List<String>,
) : ChannelEvent()

class HEnableImageSearchEvent : ChannelEvent()
class HDisableImageSearchEvent : ChannelEvent()
class HCancelImageModelDownloadEvent : ChannelEvent()

class HCancelNotificationsEvent(val ids: Set<String>) : ChannelEvent()
class HChatItemsDeletedEvent(val ids: Set<String>) : ChannelEvent()

data class HDownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()
