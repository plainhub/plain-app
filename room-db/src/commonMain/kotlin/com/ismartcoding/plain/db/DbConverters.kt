package com.ismartcoding.plain.db

import androidx.room3.ColumnTypeConverter
import com.ismartcoding.plain.enums.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val converterJson = Json { ignoreUnknownKeys = true }

class ChannelMemberListConverter {
    @ColumnTypeConverter
    fun toJSON(list: List<ChannelMember>): String {
        return converterJson.encodeToString(list)
    }

    @ColumnTypeConverter
    fun fromJSON(value: String): List<ChannelMember> {
        if (value.isEmpty()) {
            return emptyList()
        }
        return converterJson.decodeFromString<List<ChannelMember>>(value)
    }
}

class ShareRootListConverter {
    @ColumnTypeConverter
    fun toJSON(list: List<ShareRoot>): String {
        return converterJson.encodeToString(list)
    }

    @ColumnTypeConverter
    fun fromJSON(value: String): List<ShareRoot> {
        if (value.isEmpty()) {
            return emptyList()
        }
        return converterJson.decodeFromString<List<ShareRoot>>(value)
    }
}

class DateConverter {
    @ColumnTypeConverter
    fun stringFromDate(date: kotlin.time.Instant?): String? {
        return date?.toString()
    }

    @ColumnTypeConverter
    fun dateFromString(date: String?): kotlin.time.Instant? {
        return date?.let { kotlin.time.Instant.parse(it) }
    }
}

class ChatItemContentConverter {
    @ColumnTypeConverter
    fun stringTo(json: String): DMessageContent {
        return DChat.parseContent(json)
    }

    @ColumnTypeConverter
    fun dataToString(data: DMessageContent): String {
        return data.toJSONString()
    }
}

class PeerStatusConverter {
    @ColumnTypeConverter
    fun toDB(value: PeerStatus): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): PeerStatus = PeerStatus.valueOf(value)
}

class DeviceTypeConverter {
    @ColumnTypeConverter
    fun toDB(value: DeviceType): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): DeviceType = DeviceType.valueOf(value)
}

class ChannelMemberStatusConverter {
    @ColumnTypeConverter
    fun toDB(value: ChannelMemberStatus): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): ChannelMemberStatus = ChannelMemberStatus.valueOf(value)
}

class ChatChannelStatusConverter {
    @ColumnTypeConverter
    fun toDB(value: ChatChannelStatus): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): ChatChannelStatus = ChatChannelStatus.valueOf(value)
}

class ChatStatusConverter {
    @ColumnTypeConverter
    fun toDB(value: ChatStatus): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): ChatStatus = ChatStatus.valueOf(value)
}

class SessionTypeConverter {
    @ColumnTypeConverter
    fun toDB(value: SessionType): String = value.name

    @ColumnTypeConverter
    fun fromDB(value: String): SessionType = SessionType.valueOf(value)
}
