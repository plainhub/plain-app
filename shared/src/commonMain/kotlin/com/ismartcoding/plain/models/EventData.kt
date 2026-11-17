package com.ismartcoding.plain.models


import com.ismartcoding.plain.features.sessions.SessionType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("__typename")
sealed interface EventData

@Serializable
@SerialName("LoginEventData")
data class LoginEventData(val sessionType: SessionType) : EventData

@Serializable
@SerialName("LoginFailedEventData")
data class LoginFailedEventData(val reason: AuthErrorCode) : EventData

@Serializable
data class EventSession(val clientId: String, val clientName: String, val type: SessionType)

@Serializable
@SerialName("SessionEventData")
data class SessionEventData(val session: EventSession) : EventData

@Serializable
@SerialName("DeviceNameEventData")
data class DeviceNameEventData(val oldName: String, val newName: String) : EventData

@Serializable
data class EventVolume(val id: String?, val name: String?, val devicePath: String?, val mountPath: String, val fileSystem: String?)

@Serializable
data class MountFailure(val reason: MountErrorCode, val detail: String?)

@Serializable
@SerialName("VolumeEventData")
data class VolumeEventData(val volume: EventVolume) : EventData

@Serializable
@SerialName("VolumeFailedEventData")
data class VolumeFailedEventData(val volume: EventVolume, val failure: MountFailure) : EventData

@Serializable
data class DiskFormatFailure(val reason: DiskFormatErrorCode, val detail: String?)

@Serializable
data class EventDisk(val id: String?, val name: String?, val path: String, val sizeBytes: Long?)

@Serializable
@SerialName("DiskFormatEventData")
data class DiskFormatEventData(val disk: EventDisk, val fileSystem: String) : EventData

@Serializable
@SerialName("DiskFormatFailedEventData")
data class DiskFormatFailedEventData(val disk: EventDisk, val fileSystem: String, val failure: DiskFormatFailure) : EventData
