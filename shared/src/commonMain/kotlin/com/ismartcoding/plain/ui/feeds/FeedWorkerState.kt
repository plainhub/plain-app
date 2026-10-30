package com.ismartcoding.plain.ui.feeds

import androidx.compose.runtime.mutableStateMapOf
import com.ismartcoding.plain.models.FeedSyncState
import com.ismartcoding.plain.features.feeds.FeedsClient
import com.ismartcoding.plain.features.feeds.FeedWorkerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object FeedWorkerState : FeedSyncStateStore(com.ismartcoding.plain.features.feeds.FeedsClient)

open class FeedSyncStateStore internal constructor(private val client: com.ismartcoding.plain.features.feeds.FeedsApi) {
    private val lock = Mutex()
    private val states = mutableStateMapOf<String, FeedSyncState>()

    fun status(id: String): FeedWorkerStatus? = states[id]?.status

    internal fun completed(): Map<String, FeedSyncState> = states.filterValues {
        it.status in setOf(FeedWorkerStatus.COMPLETED, FeedWorkerStatus.ERROR, FeedWorkerStatus.CANCELLED)
    }

    suspend fun refresh() = lock.withLock { applyLocked(client.syncStates(), replace = true) }
    internal suspend fun apply(update: FeedSyncUpdate) = lock.withLock {
        withContext(Dispatchers.Main) { update.removed.forEach(states::remove) }
        applyLocked(update.states, replace = update.replace)
    }

    private suspend fun applyLocked(values: List<FeedSyncState>, replace: Boolean) = withContext(Dispatchers.Main) {
        if (replace) states.keys.retainAll(values.map { it.feedId }.toSet())
        values.forEach { state ->
            if (!replace && (states[state.feedId]?.revision ?: -1L) > state.revision) return@forEach
            states[state.feedId] = state
        }
    }
}

@Serializable
internal data class FeedSyncUpdate(
    val states: List<FeedSyncState>,
    val removed: List<String>,
    val replace: Boolean,
)
