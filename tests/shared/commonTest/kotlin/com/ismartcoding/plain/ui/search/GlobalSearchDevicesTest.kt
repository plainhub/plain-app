package com.ismartcoding.plain.ui.search

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.ManagedDeviceInfo
import com.ismartcoding.plain.models.Note
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerAddress
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlHttpResponse
import com.ismartcoding.plain.rust.graphql.GraphqlHttpTransport
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchDevicesTest {
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }

    @AfterTest
    fun cleanup() { stores.forEach { it.clear() }; Dispatchers.resetMain() }

    private fun model(readDevices: suspend () -> GlobalSearchDevices): GlobalSearchViewModel {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        stores.add(owner.viewModelStore)
        return ViewModelProvider.create(owner, viewModelFactory {
            initializer { GlobalSearchViewModel(readDevices, readHistory = { emptyList() }, initialDomains = listOf(GlobalSearchDomain.NOTES)) }
        })[GlobalSearchViewModel::class].also {
            it.domain.value = GlobalSearchDomain.NOTES
            it.onQueryChange("needle")
        }
    }

    @Test
    fun pages_each_device_independently_and_keeps_equal_record_ids_distinct() = runTest {
        val first = Device("first", 25)
        val second = Device("second", 3)
        val a = first.source()
        val b = second.source()
        val vm = model { GlobalSearchDevices(listOf(a, b)) }
        vm.search()
        advanceUntilIdle()
        val state = checkNotNull(vm.domainStates[GlobalSearchDomain.NOTES])
        assertEquals(28, state.total.value)
        assertEquals(23, state.loaded.value)
        assertEquals(23, state.hits.value.map { it.key }.toSet().size)
        vm.loadMore(GlobalSearchDomain.NOTES)
        advanceUntilIdle()
        assertEquals(28, state.loaded.value)
        assertEquals(28, state.hits.value.map { it.key }.toSet().size)
        assertEquals(25, state.hits.value.count { it.device === a })
        assertEquals(3, state.hits.value.count { it.device === b })
        assertEquals(listOf(0, 20), first.pages().map { it.variables.getValue("offset").jsonPrimitive.int })
        assertEquals(listOf(0), second.pages().map { it.variables.getValue("offset").jsonPrimitive.int })
        state.hits.value.forEach { assertTrue(it.key.startsWith(it.device.key + ":")) }
    }

    @Test
    fun refresh_batches_loaded_ids_and_removes_missing_results_without_crossing_devices() = runTest {
        val first = Device("first", 3)
        val second = Device("second", 2)
        val vm = model { GlobalSearchDevices(listOf(first.source(), second.source())) }
        vm.search()
        advanceUntilIdle()
        first.rows = first.rows.drop(1).map { it.copy(title = "First updated") }
        second.rows = second.rows.map { it.copy(title = "Second updated") }
        first.requests.clear()
        second.requests.clear()
        vm.refreshHits(GlobalSearchDomain.NOTES)
        advanceUntilIdle()
        val state = checkNotNull(vm.domainStates[GlobalSearchDomain.NOTES])
        assertEquals(4, state.total.value)
        assertEquals(4, state.loaded.value)
        assertEquals(2, state.hits.value.count { it.title == "First updated" })
        assertEquals(2, state.hits.value.count { it.title == "Second updated" })
        assertEquals(1, first.requests.size)
        assertEquals(1, second.requests.size)
        assertTrue(first.requests.single().variables.getValue("query").jsonPrimitive.content.contains("trash:false"))
    }

    @Test
    fun a_device_failure_keeps_other_results_and_recovery_clears_its_own_error() = runTest {
        val first = Device("first", 1)
        val second = Device("second", 1)
        val sources = listOf(first.source(), second.source())
        val vm = model { GlobalSearchDevices(sources) }
        first.failure = "no_permission"
        vm.search()
        advanceUntilIdle()
        val state = checkNotNull(vm.domainStates[GlobalSearchDomain.NOTES])
        assertEquals("no_permission", state.devices.first().error.value)
        assertEquals(1, state.hits.value.size)
        assertSame(sources.last(), state.hits.value.single().device)
        assertTrue(state.hasErrors.value)
        assertFalse(vm.empty.value)
        first.failure = null
        vm.search()
        advanceUntilIdle()
        assertEquals(2, vm.domainStates.getValue(GlobalSearchDomain.NOTES).hits.value.size)
        assertFalse(vm.domainStates.getValue(GlobalSearchDomain.NOTES).hasErrors.value)
    }

    @Test
    fun refresh_failure_is_reported_on_its_device_and_can_recover() = runTest {
        val first = Device("first", 1)
        val second = Device("second", 1)
        val sources = listOf(first.source(), second.source())
        val vm = model { GlobalSearchDevices(sources) }
        vm.search()
        advanceUntilIdle()
        first.failure = "connection_timeout"
        second.rows = second.rows.map { it.copy(title = "Live target update") }
        vm.refreshHits()
        advanceUntilIdle()
        val state = vm.domainStates.getValue(GlobalSearchDomain.NOTES)
        assertEquals("connection_timeout", state.devices.first().error.value)
        assertEquals("Live target update", state.devices.last().hits.value.single().title)
        first.failure = null
        vm.refreshHits()
        advanceUntilIdle()
        assertNull(state.devices.first().error.value)
    }

    @Test
    fun a_cancelled_query_does_not_finish_or_populate_its_replacement() = runTest {
        val device = Device("first", 1)
        val source = device.source()
        val old = CompletableDeferred<Unit>()
        val fresh = CompletableDeferred<Unit>()
        device.beforeResponse = { operation ->
            if ("noteCount(" in operation.query) {
                if ("old" in operation.variables.getValue("query").jsonPrimitive.content) old.await() else fresh.await()
            }
        }
        val vm = model { GlobalSearchDevices(listOf(source)) }
        vm.onQueryChange("needle old")
        vm.search()
        runCurrent()
        assertTrue(vm.searching.value)
        vm.onQueryChange("needle new")
        vm.search()
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertTrue(vm.searching.value)
        fresh.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.searching.value)
        assertEquals("needle new", vm.searchedQuery.value)
        assertEquals(1, device.pages().size)
        assertTrue("new" in device.pages().single().variables.getValue("query").jsonPrimitive.content)
    }

    @Test
    fun chat_snippets_include_a_long_match_after_the_chat_list_preview_boundary() = runTest {
        val device = Device("first", 0)
        val source = device.source()
        val query = "needle-long-guid-at-the-end-of-a-message"
        val text = "Earlier message text. ".repeat(10) + query + " surrounding context"
        device.chats = listOf(com.ismartcoding.plain.models.ChatItem(
            id = "owned-chat", fromId = "first", toId = "me", status = com.ismartcoding.plain.features.chat.ChatStatus.SENT,
            content = JsonHelper.jsonEncode(com.ismartcoding.plain.features.chat.models.MessageContent(
                com.ismartcoding.plain.features.chat.models.MessageType.TEXT,
                com.ismartcoding.plain.features.chat.models.MessageText(text),
            )), createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        ))
        val vm = model { GlobalSearchDevices(listOf(source)) }
        vm.domain.value = GlobalSearchDomain.CHAT
        vm.onQueryChange(query)
        vm.search()
        advanceUntilIdle()
        val hit = vm.domainStates.getValue(GlobalSearchDomain.CHAT).hits.value.single()
        assertTrue(query in hit.snippet)
        assertTrue("surrounding context" in hit.snippet)
        assertSame(source, hit.device)
    }

    private class Device(val name: String, count: Int) {
        private val now = Instant.parse("2026-01-01T00:00:00Z")
        var rows = (0 until count).map { Note("row-$it", "$name $it", "$name needle $it", null, now, now) }
        var chats = emptyList<com.ismartcoding.plain.models.ChatItem>()
        var failure: String? = null
        var beforeResponse: suspend (GraphqlOperation) -> Unit = {}
        val requests = mutableListOf<GraphqlOperation>()
        fun pages() = requests.filter { "notes(" in it.query }

        suspend fun source(): ContentSource.Remote {
            val peer = Peer(
                id = name, name = name, ip = "", status = PeerStatus.PAIRED, port = 8443,
                deviceType = DeviceType.PHONE, createdAt = now, updatedAt = now,
                publicKey = "public-$name", token = "token-$name",
                address = PeerAddress("device.test", name, "https://device.test:8443", "", ""),
            )
            val http = PeerHttpClient(peer, peers = { listOf(peer) }, clientId = { "fixture-client" },
                encrypt = { _, payload -> Base64.encode(payload.encodeToByteArray()) },
                decrypt = { _, encoded, _ ->
                    val operation = JsonHelper.jsonDecode<GraphqlOperation>(Base64.decode(encoded).decodeToString())
                    val data = if ("app {" in operation.query) {
                        JsonHelper.jsonEncodeToElement(AppData(ManagedDeviceInfo(name, listOf(Capability.NOTES), emptyList(), "app://", "/downloads", "resource-token", null)))
                    } else {
                        requests.add(operation)
                        beforeResponse(operation)
                        failure?.let { error(it) }
                        val offset = operation.variables["offset"]?.jsonPrimitive?.int ?: 0
                        val limit = operation.variables["limit"]?.jsonPrimitive?.int ?: rows.size
                        if ("chatRecords(" in operation.query || "chatCount(" in operation.query || "peers {" in operation.query) {
                            JsonHelper.jsonEncodeToElement(ChatData(chats.size, chats.drop(offset).take(limit), listOf(peer)))
                        } else JsonHelper.jsonEncodeToElement(NotesData(rows.size, rows.drop(offset).take(limit), rows.firstOrNull { it.id == operation.variables["id"]?.jsonPrimitive?.content }))
                    }
                    Base64.encode(JsonHelper.jsonEncode(GraphqlResponse(data = data.jsonObject)).encodeToByteArray())
                },
                transport = object : GraphqlHttpTransport {
                    override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?) = GraphqlHttpResponse(200, byteArrayOf(1))
                },
            )
            return ContentSource.Remote(ManagedDeviceClient.load(peer, http))
        }
    }

    @Serializable private data class ChatData(val chatCount: Int, val chatRecords: List<com.ismartcoding.plain.models.ChatItem>, val peers: List<Peer>)
    @Serializable private data class AppData(val app: ManagedDeviceInfo)
    @Serializable private data class NotesData(val noteCount: Int, val notes: List<Note>, val note: Note?)
}
