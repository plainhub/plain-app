package com.ismartcoding.plain.db

import androidx.room.TypeConverter
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import kotlinx.datetime.*

class StringListConverter {
    @TypeConverter
    fun toJSON(list: ArrayList<String>): String {
        return jsonEncode(list)
    }

    @TypeConverter
    fun fromJSON(value: String): ArrayList<String> {
        if (value.isEmpty()) {
            return arrayListOf()
        }

        return jsonDecode<ArrayList<String>>(value)
    }
}

class DateConverter {
    @TypeConverter
    fun stringFromDate(date: Instant?): String? {
        if (date == null) {
            return null
        }
        return date.toString()
    }

    @TypeConverter
    fun dateFromString(date: String?): Instant? {
        if (date == null) {
            return null
        }
        return Instant.parse(date)
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
