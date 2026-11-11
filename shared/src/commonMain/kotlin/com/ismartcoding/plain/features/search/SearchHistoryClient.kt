package com.ismartcoding.plain.features.search

import com.ismartcoding.plain.features.keyvalue.KeyValueClient

object SearchHistoryClient {
    suspend fun read(): List<String> = KeyValueClient.get("recent_searches", emptyList())

    suspend fun record(term: String) = KeyValueClient.set("recent_searches", (listOf(term) + read().filterNot { it.equals(term, ignoreCase = true) }).take(10))

    suspend fun remove(term: String) = KeyValueClient.set("recent_searches", read().filterNot { it.equals(term, ignoreCase = true) })

    suspend fun clear() = KeyValueClient.set("recent_searches", emptyList<String>())
}
