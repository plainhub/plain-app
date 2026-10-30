package com.ismartcoding.plain.ui.search

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.search.SearchHistoryClient
import com.ismartcoding.plain.i18n.devices
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.rust.graphql.GraphqlHttpException
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GlobalSearchViewModel internal constructor(
    private val readDevices: suspend () -> GlobalSearchDevices = { searchDevices() },
    readHistory: suspend () -> List<String> = SearchHistoryClient::read,
    initialDomains: List<GlobalSearchDomain> = globalSearchDomains(),
) : ViewModel() {
    val queryText = mutableStateOf("")
    val domain = mutableStateOf<GlobalSearchDomain?>(null)
    val submitted = mutableStateOf(false)
    val searching = mutableStateOf(false)
    val searchedQuery = mutableStateOf("")
    val domainStates = mutableStateMapOf<GlobalSearchDomain, GlobalSearchDomainState>()
    val recentQueries = mutableStateOf<List<String>>(emptyList())
    val domains = mutableStateOf(initialDomains)
    internal val deviceErrors = mutableStateOf<List<GlobalSearchDeviceError>>(emptyList())
    val empty = derivedStateOf {
        searchedQuery.value.isNotEmpty() && !searching.value && deviceErrors.value.isEmpty() &&
            domainStates.values.all { it.total.value == 0 && !it.hasErrors.value }
    }
    private var revision = 0L
    private var searchJob: Job? = null
    private val refreshJobs = mutableMapOf<GlobalSearchDeviceState, Job>()

    init { viewModelScope.launchSafe { recentQueries.value = readHistory() } }

    fun onQueryChange(query: String) {
        if (queryText.value == query) return
        queryText.value = query
        submitted.value = false
        resetResults()
    }
    fun setDomain(value: GlobalSearchDomain?) {
        if (domain.value == value) return
        domain.value = value
        if (queryText.value.isNotBlank()) search()
    }
    fun submit() {
        val query = queryText.value.trim()
        if (query.isEmpty()) return
        submitted.value = true
        recordRecent(query)
        search()
    }
    fun searchFromRecent(query: String) {
        queryText.value = query
        submit()
    }
    fun removeRecent(query: String) { viewModelScope.launchSafe { SearchHistoryClient.remove(query); recentQueries.value = SearchHistoryClient.read() } }
    fun clearRecent() { viewModelScope.launchSafe { SearchHistoryClient.clear(); recentQueries.value = SearchHistoryClient.read() } }
    private fun recordRecent(query: String) { viewModelScope.launchSafe { SearchHistoryClient.record(query); recentQueries.value = SearchHistoryClient.read() } }

    fun loadMore(value: GlobalSearchDomain, pageSize: Int = PAGE_SIZE) {
        val query = searchedQuery.value
        if (query.isEmpty() || query != queryText.value.trim()) return
        domainStates[value]?.devices?.filter { !it.loading.value && it.error.value == null && it.loaded.intValue < it.total.intValue }?.forEach { state ->
            state.loading.value = true
            viewModelScope.launchSafe {
                try {
                    val hits = queryDomain(value, query, pageSize, state.loaded.intValue, state.device)
                    if (domainStates[value]?.devices?.contains(state) == true) {
                        state.hits.value += hits
                        state.loaded.intValue += hits.size
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { handleDeviceError(state, error) }
                finally { state.loading.value = false }
            }
        }
    }
    private fun resetResults() {
        revision++
        searchJob?.cancel()
        refreshJobs.values.forEach { it.cancel() }
        refreshJobs.clear()
        domainStates.clear()
        deviceErrors.value = emptyList()
        searchedQuery.value = ""
        searching.value = false
    }
    fun search() {
        val query = queryText.value.trim()
        resetResults()
        if (query.isEmpty()) return
        val request = revision
        searchedQuery.value = query
        searching.value = true
        searchJob = viewModelScope.launchSafe {
            try {
                val available = readDevices()
                deviceErrors.value = available.errors
                domains.value = GlobalSearchDomain.entries.filter { entry -> available.devices.any { entry.supportedBy(it) } }
                val selected = domain.value?.let { listOf(it) } ?: domains.value
                val size = if (domain.value == null) PAGE_SIZE else SCROLL_PAGE_SIZE
                selected.forEach { entry ->
                    domainStates[entry] = GlobalSearchDomainState(available.devices.filter { entry.supportedBy(it) }.map { GlobalSearchDeviceState(it) })
                }
                coroutineScope {
                    domainStates.toMap().forEach { (entry, state) -> state.devices.forEach { device ->
                        async {
                            try {
                                device.total.intValue = countDomain(entry, query, device.device)
                                if (device.total.intValue > 0) {
                                    device.hits.value = queryDomain(entry, query, size, 0, device.device)
                                    device.loaded.intValue = device.hits.value.size
                                }
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Exception) { handleDeviceError(device, error) }
                            finally { device.loading.value = false }
                        }
                    } }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                deviceErrors.value = listOf(GlobalSearchDeviceError(com.ismartcoding.plain.ui.i18n.LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.devices), error.message ?: error.toString()))
            } finally { if (revision == request) searching.value = false }
        }
    }
    fun refreshHits(onlyDomain: GlobalSearchDomain? = null) {
        val query = searchedQuery.value
        if (query.isEmpty() || query != queryText.value.trim()) return
        domainStates.forEach { (entry, state) ->
            if (onlyDomain != null && entry != onlyDomain) return@forEach
            state.devices.forEach devices@{ device ->
                val hits = device.hits.value
                if (hits.isEmpty()) return@devices
                refreshJobs.remove(device)?.cancel()
                refreshJobs[device] = viewModelScope.launchSafe {
                    try {
                        val fresh = reloadDomainHits(entry, hits, query, device.device)
                        if (searchedQuery.value != query || queryText.value.trim() != query || domainStates[entry] !== state || device.hits.value != hits) return@launchSafe
                        if (fresh != null && fresh != hits) {
                            device.total.intValue = (device.total.intValue - hits.size + fresh.size).coerceAtLeast(0)
                            device.hits.value = fresh
                            device.loaded.intValue = fresh.size
                        }
                        device.error.value = null
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { handleDeviceError(device, error) }
                }
            }
        }
    }
    private fun handleDeviceError(state: GlobalSearchDeviceState, error: Exception) {
        if (state.device is ContentSource.Remote && error.isRemoteAccessDisabled()) {
            state.hits.value = emptyList()
            state.total.intValue = 0
            state.loaded.intValue = 0
            state.error.value = null
        } else {
            state.error.value = error.message ?: error.toString()
        }
    }
    companion object {
        const val PAGE_SIZE = 8
        const val SCROLL_PAGE_SIZE = 20
    }
}

private fun Exception.isRemoteAccessDisabled(): Boolean =
    this is GraphqlHttpException && status == 404 || message == "desktop_access_disabled"

internal suspend fun searchDevices(
    readPeers: suspend () -> List<Peer> = { GraphqlApi.execute(Query.peers()) },
    loadDevice: suspend (Peer) -> ManagedDeviceClient = { ManagedDeviceClient.load(it) },
): GlobalSearchDevices = coroutineScope {
    val remote = readPeers().filter { it.isPaired() && it.token.isNotEmpty() }.map { peer -> async {
        try {
            val client = loadDevice(peer)
            GlobalSearchDevices(listOf(ContentSource.Remote(client)))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            GlobalSearchDevices(emptyList(), if (error.isRemoteAccessDisabled()) emptyList() else listOf(GlobalSearchDeviceError(peer.name, error.message ?: error.toString())))
        }
    } }.awaitAll()
    GlobalSearchDevices(listOf(ContentSource.Local) + remote.flatMap { it.devices }, remote.flatMap { it.errors })
}
