package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.data.IData


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
