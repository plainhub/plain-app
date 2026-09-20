package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.ai.ImageSearchStatusType
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.data.DevicePlatform
import com.ismartcoding.plain.enums.WebSettingsFeature
import com.ismartcoding.plain.enums.AppChannelType
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.enums.WebsiteType
import com.ismartcoding.plain.enums.CallType
import com.ismartcoding.plain.enums.ChatChannelStatus
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.enums.MediaDataType
import com.ismartcoding.plain.enums.DriveType
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.enums.EmailType
import com.ismartcoding.plain.enums.EventType
import com.ismartcoding.plain.enums.ImProtocol
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.enums.PeerStatus
import com.ismartcoding.plain.enums.PackageType
import com.ismartcoding.plain.enums.PhoneType
import com.ismartcoding.plain.enums.PostalType
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.enums.SmsType
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.platform.DeviceFeature
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.MediaItem
import com.ismartcoding.plain.httpserver.models.MergeTaskStatus
import kotlin.time.Instant

fun SchemaBuilder.addMainSchemaTypes() {
    // Main is a superset of the peer schema (peer chat items also flow through
    // the authenticated schema), so reuse its shared types without duplicating.
    addPeerSchemaTypes()
    // Shared media interface: must be registered so Audio/Image/Video/Doc print
    // `implements MediaItem` and clients can define cross-type fragments on it.
    type<MediaItem>()
    enum<WebSettingsFeature> {
        description = "Deep-link targets on the web-access settings page; passed to openWebSettings to scroll to and highlight one option."
    }
    enum<CallType>()
    enum<SmsType>()
    enum<PhoneType>()
    enum<EmailType>()
    enum<PostalType>()
    enum<EventType>()
    enum<WebsiteType>()
    enum<ImProtocol>()
    enum<ChatChannelStatus>()
    enum<MediaPlayMode>()
    enum<DataType> {
        description = "Taggable data domains. The four media members (AUDIO/VIDEO/IMAGE/DOC) are exactly MediaDataType; a single variable cannot span both enums, so documents mixing e.g. tags(type: DataType!) with mediaBuckets(type: MediaDataType!) must declare one variable per enum."
    }
    enum<MediaDataType> {
        description = "Media-library domains — a strict subset of DataType with the same four member names."
    }
    enum<DriveType>()
    enum<DeviceType>()
    enum<DeviceFeature> {
        description = "Whether the device (server) itself provides this capability — e.g. a NAS has no SMS, so it omits SMS from App.features. Clients gate UI on App.features instead of sniffing OS versions; this says nothing about client-side access rights, which are App.permissions (Permission)."
    }
    enum<MergeTaskStatus>()
    enum<Permission> {
        description = "Client-side access rights: Android runtime permissions that gate web API access. App.permissions lists the ones currently enabled AND granted. Whether the device supports a capability at all is App.features (DeviceFeature)."
    }
    enum<FileSortBy> {
        description = "Sort orders for file/media lists. TAKEN_AT_DESC sorts by capture time and is only meaningful in taken-date grouped views; docs/packages treat it as an ordinary no-op order."
    }
    enum<PomodoroState>()
    enum<ScreenMirrorMode>()
    enum<PackageType>()
    enum<AppChannelType>()
    enum<ScreenMirrorControlAction>()
    enum<DevicePlatform>()
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
        description = "ISO-8601 / RFC 3339 UTC timestamp string, e.g. 2026-09-20T12:34:56.789Z"
        deserialize = { value: String -> Instant.parse(value) }
        serialize = Instant::toString
    }
}
