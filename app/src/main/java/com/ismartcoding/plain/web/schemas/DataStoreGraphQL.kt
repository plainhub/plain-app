package com.ismartcoding.plain.web.schemas

import androidx.datastore.preferences.core.edit
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.preferences.appDataStore
import com.ismartcoding.plain.preferences.getPreferencesAsync
import com.ismartcoding.plain.web.models.KeyValuePair

fun SchemaBuilder.addDataStoreSchema() {
    query("dataStorePath") {
        resolver { ->
            MainApp.instance.filesDir.absolutePath + "/datastore/settings.preferences_pb"
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
        resolver { key: String ->
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
