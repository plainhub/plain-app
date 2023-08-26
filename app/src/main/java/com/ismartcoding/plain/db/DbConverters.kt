package com.ismartcoding.plain.db

import androidx.room.TypeConverter
import com.ismartcoding.lib.extensions.toJSON
import com.ismartcoding.lib.extensions.toStringList
import kotlinx.datetime.*
import org.json.JSONArray

class StringListConverter {
    @TypeConverter
    fun toJSON(list: ArrayList<String>): String {
        return list.toJSON().toString()
    }

    @TypeConverter
    fun fromJSON(value: String): ArrayList<String> {
        if (value.isEmpty()) {
            return arrayListOf()
        }
        return ArrayList(JSONArray(value).toStringList())
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
        return date.toInstant()
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

