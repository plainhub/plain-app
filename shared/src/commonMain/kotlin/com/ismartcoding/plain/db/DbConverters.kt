package com.ismartcoding.plain.db

import androidx.room.TypeConverter
import com.ismartcoding.plain.enums.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val converterJson = Json { ignoreUnknownKeys = true }

class ChannelMemberListConverter {
    @TypeConverter
    fun toJSON(list: List<ChannelMember>): String {
        return converterJson.encodeToString(list)
    }

    @TypeConverter
    fun fromJSON(value: String): List<ChannelMember> {
        if (value.isEmpty()) {
            return emptyList()
        }
        return converterJson.decodeFromString<List<ChannelMember>>(value)
    }
}

class DateConverter {
    @TypeConverter
    fun stringFromDate(date: kotlin.time.Instant?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun dateFromString(date: String?): kotlin.time.Instant? {
        return date?.let { kotlin.time.Instant.parse(it) }
    }
}

class ChatItemContentConverter {
    @TypeConverter
    fun stringTo(json: String): DMessageContent {
        return DChat.parseContent(json)
    }

    @TypeConverter
    fun dataToString(data: DMessageContent): String {
        return data.toJSONString()
    }
}

class PeerStatusConverter {
    @TypeConverter
    fun toDB(value: PeerStatus): String = value.name

    @TypeConverter
    fun fromDB(value: String): PeerStatus = PeerStatus.valueOf(value)
}

class DeviceTypeConverter {
    @TypeConverter
    fun toDB(value: DeviceType): String = value.name

    @TypeConverter
    fun fromDB(value: String): DeviceType = DeviceType.valueOf(value)
}

class ChannelMemberStatusConverter {
    @TypeConverter
    fun toDB(value: ChannelMemberStatus): String = value.name

    @TypeConverter
    fun fromDB(value: String): ChannelMemberStatus = ChannelMemberStatus.valueOf(value)
}

class ChatChannelStatusConverter {
    @TypeConverter
    fun toDB(value: ChatChannelStatus): String = value.name

    @TypeConverter
    fun fromDB(value: String): ChatChannelStatus = ChatChannelStatus.valueOf(value)
}

class ChatStatusConverter {
    @TypeConverter
    fun toDB(value: ChatStatus): String = value.name

    @TypeConverter
    fun fromDB(value: String): ChatStatus = ChatStatus.valueOf(value)
}

class SessionTypeConverter {
    @TypeConverter
    fun toDB(value: SessionType): String = value.name

    @TypeConverter
    fun fromDB(value: String): SessionType = SessionType.valueOf(value)
}
