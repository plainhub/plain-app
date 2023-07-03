package com.ismartcoding.plain.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

suspend fun <T> DataStore<Preferences>.put(key: Preferences.Key<T>, value: T) {
    this.edit {
        withContext(Dispatchers.IO) {
            it[key] = value
        }
    }
}

fun <T> DataStore<Preferences>.putBlocking(key: Preferences.Key<T>, value: T) {
    runBlocking {
        this@putBlocking.edit {
            it[key] = value
        }
    }
}


@Suppress("UNCHECKED_CAST")
fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    return runBlocking {
        this@get.data.catch { exception ->
            if (exception is IOException) {
                LogCat.e("Get data store error $exception")
                exception.printStackTrace()
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map {
            it[key]
        }.first() as T
    }
}

