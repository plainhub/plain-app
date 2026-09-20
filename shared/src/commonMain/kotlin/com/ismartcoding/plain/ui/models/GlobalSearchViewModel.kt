package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.preferences.RecentSearchesPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Global search state and orchestration: query text, scope, recents and the
 * per-domain result states. How each domain is counted, queried and
 * re-synced lives in GlobalSearchDomainQueries.kt; the models in
 * GlobalSearchModels.kt; the UI in ui/page/search.
 */
class GlobalSearchViewModel : ViewModel() {
    var queryText = mutableStateOf("")
    /** null = search every domain. */
    var domain = mutableStateOf<GlobalSearchDomain?>(null)
    /** false = live suggestions while typing, true = submitted results. */
    var submitted = mutableStateOf(false)
    var searching = mutableStateOf(false)
    /** True once the current query finished with zero hits everywhere (drives the empty state). */
    var empty = mutableStateOf(false)
    /** Query the current domainStates were built for; guards re-entrant searches. */
    var searchedQuery = mutableStateOf("")
    val domainStates = mutableStateMapOf<GlobalSearchDomain, GlobalSearchDomainState>()
    var recentQueries = mutableStateOf<List<String>>(emptyList())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.getValueAsync()
        }
    }

    fun onQueryChange(q: String) {
        if (queryText.value == q) return
        queryText.value = q
        submitted.value = false
        if (q.isBlank()) resetResults()
    }

    fun setDomain(d: GlobalSearchDomain?) {
        if (domain.value == d) return
        domain.value = d
        if (queryText.value.isNotBlank()) {
            search()
        }
    }

    fun submit() {
        val q = queryText.value.trim()
        if (q.isEmpty()) return
        submitted.value = true
        recordRecent(q)
        search()
    }

    fun searchFromRecent(q: String) {
        queryText.value = q
        submitted.value = true
        recordRecent(q)
        search()
    }

    fun removeRecent(q: String) {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.removeAsync(q)
        }
    }

    fun clearRecent() {
        viewModelScope.launch {
            RecentSearchesPreference.clearAsync()
            recentQueries.value = emptyList()
        }
    }

    private fun recordRecent(q: String) {
        viewModelScope.launch {
            recentQueries.value = RecentSearchesPreference.recordAsync(q)
        }
    }

    fun loadMore(d: GlobalSearchDomain, pageSize: Int = PAGE_SIZE) {
        val q = queryText.value.trim()
        if (q.isEmpty()) return
        val state = domainStates[d] ?: return
        if (state.loading.value || state.loaded.value >= state.total.value) return
        viewModelScope.launch {
            state.loading.value = true
            try {
                val hits = queryDomain(d, q, pageSize, state.loaded.value)
                state.hits.value += hits
                state.loaded.value += hits.size
            } finally {
                state.loading.value = false
            }
        }
    }

    private fun resetResults() {
        searchJob?.cancel()
        searchJob = null
        domainStates.clear()
        searchedQuery.value = ""
        searching.value = false
        empty.value = false
    }

    fun search() {
        val q = queryText.value.trim()
        if (q.isEmpty()) {
            resetResults()
            return
        }
        searchJob?.cancel()
        searchedQuery.value = q
        val scope = domain.value
        val targets = scope?.let { listOf(it) } ?: globalSearchDomains()
        val firstPage = if (scope != null) SCROLL_PAGE_SIZE else PAGE_SIZE
        val fresh = targets.associateWith { GlobalSearchDomainState() }
        domainStates.clear()
        targets.forEach { domainStates[it] = fresh.getValue(it) }
        searching.value = true
        empty.value = false
        searchJob = viewModelScope.launch {
            try {
                coroutineScope {
                    targets.forEach { d ->
                        async {
                            val state = fresh.getValue(d)
                            state.loading.value = true
                            try {
                                val total = countDomain(d, q)
                                state.total.value = total
                                if (total > 0) {
                                    val hits = queryDomain(d, q, firstPage, 0)
                                    state.hits.value = hits
                                    state.loaded.value = hits.size
                                }
                            } finally {
                                state.loading.value = false
                            }
                        }
                    }
                }
                empty.value = fresh.values.all { it.total.intValue == 0 }
            } finally {
                searching.value = false
            }
        }
    }

    /**
     * Re-syncs loaded hits with their source data after returning from a
     * viewer page (note edited, feed entry read, media trashed, ...). Domains
     * without an editable source (files, chat, apps) are skipped. Entries
     * that no longer exist fall out of the list.
     */
    fun refreshHits() {
        val q = queryText.value.trim()
        if (q.isEmpty()) return
        domainStates.forEach { (d, state) ->
            val hits = state.hits.value
            if (hits.isEmpty()) return@forEach
            viewModelScope.launch {
                val fresh = reloadDomainHits(d, hits, q)
                if (fresh != null && fresh != hits) {
                    val removed = hits.size - fresh.size
                    if (removed > 0) state.total.intValue -= removed
                    state.hits.value = fresh
                    state.loaded.intValue = fresh.size
                }
            }
        }
    }

    companion object {
        /** Rows fetched per tap of a section's more button. */
        const val PAGE_SIZE = 8

        /** Rows fetched per scroll page; also the first page of a scoped search. */
        const val SCROLL_PAGE_SIZE = 20
    }
}
