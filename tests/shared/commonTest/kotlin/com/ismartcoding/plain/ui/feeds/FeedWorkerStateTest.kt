package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.models.FeedSyncState
import com.ismartcoding.plain.features.feeds.FeedWorkerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FeedWorkerStateTest {
    @Test
    fun delayed_completion_cannot_replace_a_newer_request_and_removal_is_projected() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val id = "feed-projection-order"
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState(id, "new-task", 20, FeedWorkerStatus.QUEUED, "")), emptyList(), replace = false))
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState(id, "old-task", 19, FeedWorkerStatus.COMPLETED, "")), emptyList(), replace = false))
            assertEquals(FeedWorkerStatus.QUEUED, FeedWorkerState.status(id))
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState(id, "new-task", 21, FeedWorkerStatus.RUNNING, "")), emptyList(), replace = false))
            assertEquals(FeedWorkerStatus.RUNNING, FeedWorkerState.status(id))
            FeedWorkerState.apply(FeedSyncUpdate(emptyList(), listOf(id), replace = false))
            assertNull(FeedWorkerState.status(id))
        } finally {
            Dispatchers.resetMain()
        }
    }
    @Test
    fun host_snapshot_replaces_states_left_over_from_a_disconnected_host() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState("retired", "old", 1, FeedWorkerStatus.RUNNING, "")), emptyList(), replace = true))
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState("current", "new", 2, FeedWorkerStatus.COMPLETED, "")), emptyList(), replace = true))
            assertNull(FeedWorkerState.status("retired"))
            assertEquals(FeedWorkerStatus.COMPLETED, FeedWorkerState.status("current"))
            assertEquals(2L, FeedWorkerState.completed().getValue("current").revision)
            FeedWorkerState.apply(FeedSyncUpdate(listOf(FeedSyncState("current", "new", 3, FeedWorkerStatus.CANCELED, "")), emptyList(), replace = false))
            assertEquals(FeedWorkerStatus.CANCELED, FeedWorkerState.status("current"))
            assertEquals(3L, FeedWorkerState.completed().getValue("current").revision)
            FeedWorkerState.apply(FeedSyncUpdate(emptyList(), emptyList(), replace = true))
        } finally {
            Dispatchers.resetMain()
        }
    }

}
