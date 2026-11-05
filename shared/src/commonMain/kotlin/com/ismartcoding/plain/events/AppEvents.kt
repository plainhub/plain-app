package com.ismartcoding.plain.events

import com.ismartcoding.plain.rust.native.RustHostReports
import com.ismartcoding.plain.features.files.ExportFileType
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.features.files.PickFileType
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.ChannelEvent
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.platform.media.restartAudioIfPlaying
import com.ismartcoding.plain.platform.permissions.Permission

class WindowFocusChangedEvent(val hasFocus: Boolean) : ChannelEvent()
class ExportFileEvent(val type: ExportFileType, val fileName: String) : ChannelEvent()
class ExportFileResultEvent(val type: ExportFileType, val uri: String) : ChannelEvent()
class PickFileEvent(val tag: PickFileTag, val type: PickFileType, val multiple: Boolean) : ChannelEvent()
class PickFileResultEvent(val tag: PickFileTag, val type: PickFileType, val uris: Set<String>) : ChannelEvent()

object AppEvents {
    fun register() = coMain {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is WindowFocusChangedEvent -> if (event.hasFocus) {
                    coIO { RustHostReports.permissions("true") }
                }
                is PermissionsResultEvent -> {
                    if (event.map.containsKey(Permission.POST_NOTIFICATIONS.toSysPermission())) {
                        coMain { restartAudioIfPlaying() }
                    }
                    coIO {
                        RustHostReports.permissions("true")
                    }
                }
            }
        }
    }
}
