package com.ismartcoding.plain.preferences

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RemoteSetting<T> internal constructor(
    private val client: PreferencesClient,
    private val select: (UserSettings) -> T,
    private val patch: (T) -> UserSettingsPatch,
) : StateFlow<T> {
    override val value: T
        get() = select(client.user.value)
    suspend fun set(value: T) { client.patchUser(patch(value)) }
    override val replayCache: List<T> get() = listOf(value)
    @OptIn(InternalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        client.user.map(select).distinctUntilChanged().collect(collector)
        error("Settings projection completed")
    }
}
