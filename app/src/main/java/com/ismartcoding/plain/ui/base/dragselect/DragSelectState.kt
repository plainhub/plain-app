package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.data.IData


@Composable
public fun rememberDragSelectState(
    lazyGridState: LazyGridState = rememberLazyGridState(),
    initialSelection: List<String> = emptyList(),
): DragSelectState {
    val dragState = rememberSaveable(saver = DragState.Saver) { DragState() }

    return remember(lazyGridState) {
        DragSelectState(
            initialSelection = initialSelection,
            gridState = { lazyGridState },
            listState = { null },
            dragState = dragState,
        )
    }
}

@Composable
public fun rememberDragSelectState(
    lazyGridState: () -> LazyGridState?,
    initialSelection: List<String> = emptyList(),
): DragSelectState {
    val dragState = rememberSaveable(saver = DragState.Saver) { DragState() }

    return remember(lazyGridState) {
        DragSelectState(
            initialSelection = initialSelection,
            gridState = { lazyGridState() },
            listState = { null },
            dragState = dragState,
        )
    }
}

@Composable
public fun rememberListDragSelectState(
    lazyListState: LazyListState = rememberLazyListState(),
    initialSelection: List<String> = emptyList(),
): DragSelectState {
    val dragState = rememberSaveable(saver = DragState.Saver) { DragState() }

    return remember(lazyListState) {
        DragSelectState(
            initialSelection = initialSelection,
            gridState = { null },
            listState = { lazyListState },
            dragState = dragState,
        )
    }
}

@Composable
public fun rememberListDragSelectState(
    lazyListState: () -> LazyListState?,
    initialSelection: List<String> = emptyList(),
): DragSelectState {
    val dragState = rememberSaveable(saver = DragState.Saver) { DragState() }

    return remember(lazyListState) {
        DragSelectState(
            initialSelection = initialSelection,
            gridState = { null },
            listState = { lazyListState() },
            dragState = dragState,
        )
    }
}

@Suppress("MemberVisibilityCanBePrivate")
@Stable
class DragSelectState(
    initialSelection: List<String>,
    val gridState: () -> LazyGridState?,
    val listState: () -> LazyListState?,
    var dragState: DragState,
) {
    var selectedIds: List<String> by mutableStateOf(initialSelection)
        private set
    var selectMode: Boolean by mutableStateOf(false)

    val autoScrollSpeed = mutableFloatStateOf(0f)

    fun whenDragging(
        block: DragSelectState.(dragState: DragState) -> Unit,
    ) {
        if (dragState.isDragging) {
            block(this, dragState)
        }
    }

    fun updateDrag(current: Int) {
        dragState = dragState.copy(current = current)
    }

    fun startDrag(id: String, index: Int) {
        dragState = DragState(initialId = id, initial = index, current = index)
        select(id)
    }

    fun enterSelectMode() {
        selectMode = true
    }

    fun exitSelectMode() {
        selectedIds = emptyList()
        selectMode = false
    }

    fun toggleSelectionMode() {
        if (selectMode) {
            exitSelectMode()
        } else {
            enterSelectMode()
        }
    }

    fun isSelected(id: String): Boolean = selectedIds.contains(id)

    fun updateSelected(ids: List<String>) {
        selectedIds = ids
    }

    fun select(id: String) {
        if (isSelected(id)) {
            removeSelected(id)
        } else {
            addSelected(id)
        }
    }

    fun addSelected(id: String) {
        if (!isSelected(id)) {
            selectedIds += id
        }
    }

    fun removeSelected(id: String) {
        if (selectedIds.contains(id)) {
            selectedIds -= id
        }
    }

    internal fun stopDrag() {
        dragState = dragState.copy(initial = DragState.NONE)
        autoScrollSpeed.value = 0f
    }

    fun isAllSelected(allItems: List<IData>): Boolean {
        return selectedIds.size == allItems.size
    }

    fun toggleSelectAll(allItems: List<IData>) {
        selectedIds = if (isAllSelected(allItems)) {
            emptyList()
        } else {
            allItems.map { it.id }
        }
    }

    fun showBottomActions(): Boolean {
        return selectMode && selectedIds.isNotEmpty()
    }
}
