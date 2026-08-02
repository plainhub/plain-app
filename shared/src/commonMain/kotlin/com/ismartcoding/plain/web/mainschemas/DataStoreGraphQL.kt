package com.ismartcoding.plain.web.mainschemas

import androidx.datastore.preferences.core.edit
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.dataStoreFilePath
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.web.models.KeyValuePair

@GraphQLQuery
suspend fun dataStorePath(): String {
    return dataStoreFilePath()
}

@GraphQLQuery
suspend fun dataStoreEntries(): List<KeyValuePair> {
    val prefs = getPreferencesAsync()
    return prefs.asMap().map { (key, value) ->
        KeyValuePair(key.name, value.toString())
    }.sortedBy { it.key }
}

@GraphQLMutation
suspend fun deleteDataStoreEntry(key: String): Boolean {
    appDataStore.edit { prefs ->
        val target = prefs.asMap().keys.find { it.name == key }
        if (target != null) {
            prefs.remove(target)
        }
    }
    return true
}

fun SchemaBuilder.addDataStoreSchema() {
}
