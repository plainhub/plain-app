package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.enums.ChatChannelStatus
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.DriveType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.httpserver.models.BatteryHealth
import com.ismartcoding.plain.httpserver.models.BatteryPlugged
import com.ismartcoding.plain.httpserver.models.BatteryStatus
import com.ismartcoding.plain.httpserver.models.ID
import kotlin.time.Instant

fun SchemaBuilder.addSchemaTypes() {
    enum<ChatStatus>()
    enum<ChatChannelStatus>()
    enum<ChannelSystemMessageType>()
    enum<MediaPlayMode>()
    enum<DataType>()
    enum<DriveType>()
    enum<DeviceType>()
    enum<Permission>()
    enum<FileSortBy>()
    enum<PomodoroState>()
    enum<ScreenMirrorMode>()
    enum<ScreenMirrorControlAction>()
    enum<DevicePlatform>()
    enum<BatteryHealth>()
    enum<BatteryStatus>()
    enum<PeerStatus>()
    enum<BatteryPlugged>()
    enum<DiscoveryMethod>()
    enum<ChannelMemberStatus>()
    stringScalar<Instant> {
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
    stringScalar<ID> {
        deserialize = { it: String -> ID(it) }
        serialize = { it: ID -> it.toString() }
    }
}
