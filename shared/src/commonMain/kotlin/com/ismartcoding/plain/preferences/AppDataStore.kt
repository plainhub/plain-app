package com.ismartcoding.plain.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private lateinit var _appDataStore: DataStore<Preferences>

fun initDataStore(store: DataStore<Preferences>) {
    _appDataStore = store
}

val appDataStore: DataStore<Preferences>
    get() = _appDataStore

suspend fun getPreferencesAsync(): Preferences = appDataStore.dataFlow.first()

val DataStore<Preferences>.dataFlow: Flow<Preferences>
    get() = data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
