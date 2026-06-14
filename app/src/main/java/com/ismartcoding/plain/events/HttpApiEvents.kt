package com.ismartcoding.plain.events

import com.ismartcoding.lib.channel.ChannelEvent
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.db.DChat


// H prefix means event from http api
class HMessageCreatedEvent(val fromId: String, val items: List<DChat>) : ChannelEvent()

class HMessageUpdatedEvent(val id: String) : ChannelEvent()

// Download events
class HDownloadTaskDoneEvent(val downloadTask: DownloadTask) : ChannelEvent()

// Pomodoro events
class HPomodoroStartEvent(val timeLeft: Int) : ChannelEvent()

class HPomodoroPauseEvent : ChannelEvent()

class HPomodoroStopEvent : ChannelEvent()

class HStartScreenMirrorEvent(val audio: Boolean) : ChannelEvent()

class HRequestScreenMirrorAudioEvent : ChannelEvent()

class HOpenAccessibilitySettingsEvent : ChannelEvent()

class HOpenWebSettingsEvent : ChannelEvent()

class HRetryChatItemEvent(val id: String) : ChannelEvent()
/**
 * Fired after the default SMS app is launched for an MMS send.
 * AppEvents will poll content://mms until the row appears, then
 * remove the pending entry from TempData, delete the attachment
 * files on device, and emit MMS_SENT to all web clients.
 */
data class HStartMmsPollingEvent(
    val pendingId: String,
    val launchTimeSec: Long,
    val attachmentPaths: List<String>,
) : ChannelEvent()

class HEnableImageSearchEvent : ChannelEvent()
class HDisableImageSearchEvent : ChannelEvent()
class HCancelImageModelDownloadEvent : ChannelEvent()

class HCancelNotificationsEvent(val ids: Set<String>) : ChannelEvent()
