package com.ismartcoding.plain.events

import com.ismartcoding.plain.models.EventRecord
import com.ismartcoding.plain.models.EventType
import com.ismartcoding.plain.ui.settings.EventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    @Test
    fun paginationSearchAndFailureUseTheVisibleState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<Pair<String, Int>>()
            var fail = false
            val model = EventsViewModel { query, offset, _ ->
                calls.add(query to offset)
                if (fail) error("unavailable")
                if (offset == 0) List(50) { index -> EventRecord("$index", EventType.LOGIN, "Fixture", null, Instant.parse("2026-01-01T00:00:00Z")) } else emptyList()
            }
            model.refresh(); advanceUntilIdle()
            assertTrue(model.hasMore)
            model.loadMore(); advanceUntilIdle()
            assertEquals("" to 50, calls.last())
            assertFalse(model.hasMore)
            fail = true
            model.search("fixture"); advanceUntilIdle()
            assertEquals("text:\"fixture\"" to 0, calls.last())
            assertEquals("unavailable", model.error)
            assertTrue(model.entries.isEmpty())
        } finally { Dispatchers.resetMain() }
    }
    @Test
    fun overlappingPagesKeepUniqueRowsAndAdvanceTheServerOffset() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val offsets = mutableListOf<Int>()
            val model = EventsViewModel { _, offset, _ ->
                offsets.add(offset)
                val start = if (offset == 0) 0 else 49
                if (offset >= 100) emptyList() else List(50) { index ->
                    EventRecord("${index + start}", EventType.LOGIN, "Fixture", null, Instant.parse("2026-01-01T00:00:00Z"))
                }
            }
            model.refresh(); advanceUntilIdle()
            model.loadMore(); advanceUntilIdle()
            assertEquals(99, model.entries.size)
            model.loadMore(); advanceUntilIdle()
            assertEquals(listOf(0, 50, 100), offsets)
        } finally { Dispatchers.resetMain() }
    }
}
