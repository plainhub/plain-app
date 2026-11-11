package com.ismartcoding.plain.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.models.EventRecord
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EventsViewModel(
    private val read: suspend (String, Int, Int) -> List<EventRecord> = { query, offset, limit ->
        withIO { GraphqlApi.execute(Query.events(query, offset, limit)) }
    },
) : ViewModel() {
    var search by mutableStateOf("")
        private set
    var entries by mutableStateOf(emptyList<EventRecord>())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf("")
        private set
    var hasMore by mutableStateOf(false)
        private set
    private var generation = 0
    private var nextOffset = 0
    private var request: Job? = null

    fun search(value: String) { search = value; refresh() }
    fun refresh() = load(true)
    fun loadMore() = load(false)

    private fun load(reset: Boolean) {
        if (loading && !reset) return
        if (reset) request?.cancel()
        val ticket = ++generation
        val offset = if (reset) 0 else nextOffset
        val query = search.trim().let { if (it.isEmpty()) "" else "text:${JsonHelper.jsonEncode(it)}" }
        if (reset) { entries = emptyList(); hasMore = false }
        loading = true
        error = ""
        request = viewModelScope.launch {
            try {
                val page = read(query, offset, 50)
                if (ticket != generation) return@launch
                entries = if (reset) page else (entries + page).distinctBy { it.id }
                nextOffset = offset + page.size
                hasMore = page.size == 50
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (cause: Exception) {
                if (ticket == generation) error = cause.message ?: cause.toString()
            } finally {
                if (ticket == generation) loading = false
            }
        }
    }
}
