package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.ai.ImageSearchStatusType
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.enums.AppChannelType
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
import com.ismartcoding.plain.enums.PackageType
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

fun SchemaBuilder.addMainSchemaTypes() {
    // Main is a superset of the peer schema (peer chat items also flow through
    // the authenticated schema), so reuse its shared types without duplicating.
    addPeerSchemaTypes()
    enum<ChatChannelStatus>()
    enum<MediaPlayMode>()
    enum<DataType>()
    enum<DriveType>()
    enum<DeviceType>()
    enum<Permission>()
    enum<FileSortBy>()
    enum<PomodoroState>()
    enum<ScreenMirrorMode>()
    enum<PackageType>()
    enum<AppChannelType>()
    enum<ScreenMirrorControlAction>()
    enum<DevicePlatform>()
    enum<BatteryHealth>()
    enum<BatteryStatus>()
    enum<BatteryPlugged>()
    enum<DiscoveryMethod>()
    enum<ChannelMemberStatus>()
    enum<ImageSearchStatusType>()
}

/**
 * Types used by the peer-chat schema (`/peer_graphql`): the [ChatItem] result
 * references [ID], [Instant], [ChatStatus], and the `ChatItemContent` union;
 * the `channelSystemMessage` mutation takes a [ChannelSystemMessageType].
 */
fun SchemaBuilder.addPeerSchemaTypes() {
    enum<PeerStatus>()
    enum<ChatStatus>()
    enum<ChannelSystemMessageType>()
    stringScalar<ID> {
        // Scalar names must be compile-time fixed: the DSL defaults to
        // KClass.simpleName, which R8 renames in release builds (e.g. "p94").
        name = "ID"
        deserialize = { it: String -> ID(it) }
        serialize = { it: ID -> it.toString() }
    }
    stringScalar<Instant> {
        name = "Instant"
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
}
