package com.ismartcoding.plain.db

import androidx.room.TypeConverter
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
