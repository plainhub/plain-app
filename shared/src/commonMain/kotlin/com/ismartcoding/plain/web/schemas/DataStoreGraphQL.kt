package com.ismartcoding.plain.web.schemas

import androidx.datastore.preferences.core.edit
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.platform.dataStoreFilePath
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.web.models.KeyValuePair

fun SchemaBuilder.addDataStoreSchema() {
    query("dataStorePath") {
        resolver { ->
            dataStoreFilePath()
        }
    }

    query("dataStoreEntries") {
        resolver { ->
            val prefs = getPreferencesAsync()
            prefs.asMap().map { (key, value) ->
                KeyValuePair(key.name, value.toString())
            }.sortedBy { it.key }
        }
    }

    mutation("deleteDataStoreEntry") {
        resolver("key") { key: String ->
            appDataStore.edit { prefs ->
                val target = prefs.asMap().keys.find { it.name == key }
                if (target != null) {
                    prefs.remove(target)
                }
            }
            true
        }
    }
}
