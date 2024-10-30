package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ismartcoding.plain.data.IData
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

fun Modifier.listDragSelect(
    items: List<IData>,
    state: DragSelectState,
    enableAutoScroll: Boolean = true,
    autoScrollThreshold: Float? = null,
    enableHaptics: Boolean = true,
    hapticFeedback: HapticFeedback? = null,
): Modifier = composed {
    val localHapticFeedback = LocalHapticFeedback.current
    val scrollThreshold = autoScrollThreshold ?: DragSelectDefaults.autoScrollThreshold
    
    if (enableAutoScroll) {
        LaunchedEffect(state.autoScrollSpeed.floatValue) {
            if (state.autoScrollSpeed.floatValue == 0f) return@LaunchedEffect

            val listState = state.listState() ?: return@LaunchedEffect
            
            while (isActive) {
                listState.scrollBy(state.autoScrollSpeed.floatValue)
                delay(10)
            }
        }
    }

    val haptics = if (enableHaptics) hapticFeedback ?: localHapticFeedback else null

    if (!state.selectMode) {
        return@composed this
    }
    
    pointerInput(state.selectMode) {
        detectDragGestures(
            onDragStart = { offset ->
                val lazyListState = state.listState() ?: return@detectDragGestures
                val itemIndex = findItemAt(lazyListState, offset.y)
                if (itemIndex != -1) {
                    val item = items.getOrNull(itemIndex)
                    if (item != null) {
                        haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.startDrag(item.id, itemIndex)
                        state.addSelected(item.id)
                    }
                }
            },
            onDragCancel = state::stopDrag,
            onDragEnd = state::stopDrag,
            onDrag = { change, _ ->
                state.whenDragging { dragState ->
                    val lazyListState = state.listState() ?: return@whenDragging
                    
                    // 更新自动滚动速度
                    val viewportHeight = lazyListState.layoutInfo.viewportSize.height
                    autoScrollSpeed.value = when {
                        change.position.y < scrollThreshold -> -scrollThreshold * 0.2f
                        change.position.y > viewportHeight - scrollThreshold -> scrollThreshold * 0.2f
                        else -> 0f
                    }
                    
                    // 查找当前位置的项目索引
                    val itemIndex = findItemAt(lazyListState, change.position.y)
                    if (itemIndex == -1 || itemIndex == dragState.current) {
                        return@whenDragging
                    }
                    
                    // 获取拖拽范围内的所有项目索引
                    val start = minOf(itemIndex, dragState.initial)
                    val end = maxOf(itemIndex, dragState.initial)
                    val inRangeIndices = (start..end).toList()
                    
                    // 更新选中状态
                    val shouldSelect = true
                    for (index in inRangeIndices) {
                        val id = items.getOrNull(index)?.id ?: continue
                        if (shouldSelect) {
                            state.addSelected(id)
                        } else {
                            state.removeSelected(id)
                        }
                    }
                    
                    this.dragState = dragState.copy(current = itemIndex)
                }
            }
        )
    }
}

private fun findItemAt(listState: LazyListState, y: Float): Int {
    val items = listState.layoutInfo.visibleItemsInfo
    if (items.isEmpty()) return -1
    // Select the item whose center Y is closest to the touch position
    return items.minByOrNull { abs((it.offset + it.size / 2f) - y) }?.index ?: -1
} 