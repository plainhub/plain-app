package com.ismartcoding.lib.extensions

import org.json.JSONArray
import org.json.JSONObject

interface IJSONItem {
    fun toJSON(): JSONObject
}

fun <T : Any> List<T>.toJSON(): JSONArray {
    val jsonArr = JSONArray()
    this.forEach {
        jsonArr.put(
            if (it is IJSONItem) {
                it.toJSON()
            } else {
                it.toString()
            },
        )
    }
    return jsonArr
}

fun JSONArray.toStringList(): List<String> {
    val list = mutableListOf<String>()
    for (i in 0 until this.length()) {
        list.add(this.optString(i))
    }
    return list
}

fun <T : IJSONItem> JSONArray.parse(toItem: (JSONObject) -> T): List<T> {
    val items = mutableListOf<T>()
    for (i in 0 until this.length()) {
        items.add(toItem(this.getJSONObject(i)))
    }

    return items
}
