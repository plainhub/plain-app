package com.ismartcoding.plain.ui.images.editor

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ismartcoding.plain.lib.yjs.CanvasSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorHistoryTest {
    @Test
    fun canvasSettingsUndoAndRedoRestoreTheWholeDocument() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val vm = ViewModelProvider.create(store, viewModelFactory { initializer { ImageEditorEditViewModel(com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient(com.ismartcoding.plain.rust.graphql.GraphqlApi)) } })[ImageEditorEditViewModel::class]
            val initial = vm.document.value.canvasSize
            vm.setCanvasSize(640, 480)
            vm.undo()
            assertEquals(initial, vm.document.value.canvasSize)
            vm.redo()
            assertEquals(CanvasSize(640, 480), vm.document.value.canvasSize)
        } finally { store.clear(); Dispatchers.resetMain() }
    }
    @Test
    fun recreatingThePageKeepsTheUnsavedDocumentAndUndoHistory() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val vm = ViewModelProvider.create(store, viewModelFactory { initializer { ImageEditorEditViewModel(com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient(com.ismartcoding.plain.rust.graphql.GraphqlApi)) } })[ImageEditorEditViewModel::class]
            vm.loadAsync("")
            vm.setCanvasSize(640, 480)
            vm.loadAsync("")
            assertEquals(CanvasSize(640, 480), vm.document.value.canvasSize)
            assertTrue(vm.dirty.value)
            assertTrue(vm.canUndo.value)
            vm.undo()
            assertEquals(CanvasSize(1920, 1080), vm.document.value.canvasSize)
        } finally { store.clear(); Dispatchers.resetMain() }
    }

}
