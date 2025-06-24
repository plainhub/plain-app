package com.ismartcoding.plain.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { 
            LogCat.e("DataStore preferences corrupted, creating new empty preferences")
            emptyPreferences() 
        }
    )
)

suspend fun <T> DataStore<Preferences>.put(
    key: Preferences.Key<T>,
    value: T,
) {
    this.edit {
        it[key] = value
    }
}

@Suppress("UNCHECKED_CAST")
suspend fun <T> DataStore<Preferences>.getAsync(key: Preferences.Key<T>): T? {
    return data.catch { exception ->
        if (exception is IOException) {
            LogCat.e("Get data store error $exception")
            exception.printStackTrace()
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.first()[key] as T
}

suspend fun DataStore<Preferences>.getPreferencesAsync(): Preferences {
    return data.catch { exception ->
        if (exception is IOException) {
            LogCat.e("Get data store preferences error $exception")
            exception.printStackTrace()
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.first()
}

val DataStore<Preferences>.dataFlow: Flow<Preferences>
    get() = data.catch { exception ->
        if (exception is IOException) {
            LogCat.e("Get data store flow error $exception")
            exception.printStackTrace()
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
