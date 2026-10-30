package com.ismartcoding.plain.ui.tools

import com.ismartcoding.plain.features.notes.NotesApi
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ToolsViewModelTest {
    @Test
    fun feature_order_and_drafts_use_their_owning_device_client() = runTest {
        val first = Device("NOTES|FILES", "first draft")
        val second = Device("FILES|NOTES", "second draft")
        val firstTools = ToolsViewModel(first.api, kotlinx.coroutines.flow.emptyFlow())
        val secondTools = ToolsViewModel(second.api, kotlinx.coroutines.flow.emptyFlow())
        firstTools.load()
        secondTools.load()
        assertEquals(listOf("NOTES", "FILES"), firstTools.enabledFeatures.value)
        assertEquals(listOf("FILES", "NOTES"), secondTools.enabledFeatures.value)
        firstTools.save(listOf("FILES"))
        assertEquals("FILES", first.features)
        assertEquals("FILES|NOTES", second.features)
        assertEquals(listOf("FILES", "NOTES"), secondTools.enabledFeatures.value)
        val firstNotes = NotesApi(first.api)
        val secondNotes = NotesApi(second.api)
        assertEquals("first draft", firstNotes.readDraft())
        assertEquals("second draft", secondNotes.readDraft())
        secondNotes.writeDraft("updated second draft")
        assertEquals("first draft", first.draft)
        assertEquals("updated second draft", second.draft)
        assertEquals(listOf("prefs", "setPref", "savedValue"), first.requests.map { operation ->
            when { "setPref(" in operation.query -> "setPref"; "savedValue(" in operation.query -> "savedValue"; else -> "prefs" }
        })
    }

    @Test
    fun failed_save_keeps_the_loaded_order_and_propagates_the_error() = runTest {
        val device = Device("NOTES|FILES", "draft")
        val tools = ToolsViewModel(device.api, kotlinx.coroutines.flow.emptyFlow())
        tools.load()
        device.failWrites = true
        assertEquals("no_permission", assertFailsWith<IllegalStateException> { tools.save(listOf("FILES")) }.message)
        assertEquals(listOf("NOTES", "FILES"), tools.enabledFeatures.value)
        assertEquals("NOTES|FILES", device.features)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun preference_updates_reload_only_the_selected_device() = runTest {
        val device = Device("NOTES", "")
        val changes = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
        val tools = ToolsViewModel(device.api, changes)
        backgroundScope.launch { tools.observe() }
        runCurrent()
        assertEquals(listOf("NOTES"), tools.enabledFeatures.value)
        device.features = "FILES|NOTES"
        changes.emit(Unit)
        runCurrent()
        assertEquals(listOf("FILES", "NOTES"), tools.enabledFeatures.value)
    }

    @Test
    fun rename_uses_the_selected_api_without_changing_the_other_device() = runTest {
        val first = Device("NOTES", "")
        val second = Device("NOTES", "")
        assertTrue(second.api.execute(Mutation.updateDeviceName("Selected device")))
        assertEquals("Selected device", second.name)
        assertEquals("Original device", first.name)
        assertTrue(first.requests.isEmpty())
    }

    @Serializable private data class DraftData(val savedValue: String)
    @Serializable private data class PrefsData(val prefs: Map<String, String>)

    private class Device(var features: String, var draft: String) {
        var name = "Original device"
        var failWrites = false
        val requests = mutableListOf<GraphqlOperation>()
        val api = GraphqlClient("fixture") { operation ->
            requests += operation
            val data = when {
                "setPref(" in operation.query -> {
                    if (failWrites) error("no_permission")
                    assertEquals(PrefKeys.APP_HOME_FEATURES, operation.variables.getValue("key").jsonPrimitive.content)
                    features = operation.variables.getValue("value").jsonPrimitive.content
                    JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"setPref":true}}""")
                }
                "setSavedValue(" in operation.query -> {
                    assertEquals("quick_note_draft", operation.variables.getValue("key").jsonPrimitive.content)
                    draft = operation.variables.getValue("value").jsonPrimitive.content
                    JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"setSavedValue":true}}""")
                }
                "savedValue(" in operation.query -> GraphqlResponse(data = JsonHelper.jsonEncodeToElement(DraftData(draft)).jsonObject)
                "updateDeviceName(" in operation.query -> {
                    name = operation.variables.getValue("name").jsonPrimitive.content
                    JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"updateDeviceName":true}}""")
                }
                else -> GraphqlResponse(data = JsonHelper.jsonEncodeToElement(PrefsData(mapOf(PrefKeys.APP_HOME_FEATURES to features))).jsonObject)
            }
            data
        }
    }
}
