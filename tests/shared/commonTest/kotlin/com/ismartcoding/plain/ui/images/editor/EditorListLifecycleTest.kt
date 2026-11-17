package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.rust.graphql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import kotlin.test.*

class EditorListLifecycleTest {
    @Test
    fun attachmentOwnsInitialReconnectAndReactivationReadsWithoutIdlePolling() = runTest {
        val calls = MutableStateFlow(0)
        val api = GraphqlClient("owned-editor-list") {
            calls.value++
            response(calls.value)
        }
        val vm = ImageEditorViewModel(ImageEditorProjectsClient(api))
        val updates = MutableSharedFlow<SubscriptionUpdate<*>>()
        var observer = backgroundScope.launch { vm.observe(updates) }
        runCurrent()
        updates.emit(SubscriptionUpdate.Attached)
        vm.itemsFlow.first { it.singleOrNull()?.canvasWidth == 101 }
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, calls.value)
        updates.emit(SubscriptionUpdate.Data(true))
        vm.itemsFlow.first { it.singleOrNull()?.canvasWidth == 102 }
        observer.cancelAndJoin()
        updates.emit(SubscriptionUpdate.Data(true)); advanceTimeBy(60_000); runCurrent()
        assertEquals(2, calls.value)
        observer = backgroundScope.launch { vm.observe(updates) }
        runCurrent(); updates.emit(SubscriptionUpdate.Attached)
        vm.itemsFlow.first { it.singleOrNull()?.canvasWidth == 103 }
        updates.emit(SubscriptionUpdate.Attached)
        vm.itemsFlow.first { it.singleOrNull()?.canvasWidth == 104 }
        assertEquals(4, calls.value)
        observer.cancelAndJoin()
    }

    @Test
    fun lateResponsesFromACanceledPageCannotReplaceTheReactivatedSnapshot() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val api = GraphqlClient("owned-editor-late-response") {
            val number = ++calls
            if (number == 1) withContext(NonCancellable) { started.complete(Unit); release.await() }
            response(number)
        }
        val vm = ImageEditorViewModel(ImageEditorProjectsClient(api))
        val seen = mutableListOf<Int>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.itemsFlow.collect { rows -> rows.singleOrNull()?.let { seen.add(it.canvasWidth) } } }
        val updates = MutableSharedFlow<SubscriptionUpdate<*>>()
        val old = backgroundScope.launch { vm.observe(updates) }
        runCurrent(); updates.emit(SubscriptionUpdate.Attached); started.await()
        old.cancel()
        val current = backgroundScope.launch { vm.observe(updates) }
        runCurrent(); updates.emit(SubscriptionUpdate.Attached)
        release.complete(Unit)
        vm.itemsFlow.first { it.singleOrNull()?.canvasWidth == 102 }
        old.join(); runCurrent()
        assertFalse(101 in seen)
        assertEquals(102, vm.itemsFlow.value.single().canvasWidth)
        current.cancelAndJoin()
    }

    private fun response(number: Int): GraphqlResponse {
        val row = ImageEditorProject("019f4b00-0000-7000-8000-000000000001", canvasWidth = 100 + number, canvasHeight = 64)
        return GraphqlResponse(data = JsonObject(mapOf("imageEditorProjects" to JsonArray(listOf(JsonHelper.jsonEncodeToElement(row))))))
    }
}
