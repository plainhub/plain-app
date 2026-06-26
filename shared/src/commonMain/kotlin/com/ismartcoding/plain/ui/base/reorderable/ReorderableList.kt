package com.ismartcoding.plain.ui.base.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.zIndex

@Composable
fun <T> ReorderableColumn(
    list: List<T>, onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier, verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start, onMove: () -> Unit = {},
    content: @Composable ReorderableScope.(index: Int, item: T, isDragging: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val spacing = with(LocalDensity.current) { verticalArrangement.spacing.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val state = remember(list, spacing) {
        ReorderableListState(list.size, spacing, onMove, onSettle, coroutineScope, Orientation.Vertical, layoutDirection)
    }
    Column(modifier = modifier, verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment) {
        list.forEachIndexed { i, item ->
            val isDragging by state.isItemDragging(i)
            val isAnimating by state.isItemAnimating(i)
            Box(modifier = Modifier
                .onGloballyPositioned { state.itemIntervals[i] = ItemInterval(start = it.positionInParent().y, size = it.size.height) }
                .graphicsLayer { translationY = state.itemOffsets[i].value }
                .zIndex(if (isAnimating) 1f else 0f)
            ) { ReorderableScopeImpl(state, Orientation.Vertical, i).content(i, item, isDragging) }
        }
    }
}

@Composable
fun <T> ReorderableRow(
    list: List<T>, onSettle: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top, onMove: () -> Unit = {},
    content: @Composable ReorderableScope.(index: Int, item: T, isDragging: Boolean) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val spacing = with(LocalDensity.current) { horizontalArrangement.spacing.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val state = remember(list, spacing) {
        ReorderableListState(list.size, spacing, onMove, onSettle, coroutineScope, Orientation.Horizontal, layoutDirection)
    }
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement, verticalAlignment = verticalAlignment) {
        list.forEachIndexed { i, item ->
            val isDragging by state.isItemDragging(i)
            val isAnimating by state.isItemAnimating(i)
            Box(modifier = Modifier
                .onGloballyPositioned { state.itemIntervals[i] = ItemInterval(start = it.positionInParent().x, size = it.size.width) }
                .graphicsLayer { translationX = state.itemOffsets[i].value }
                .zIndex(if (isAnimating) 1f else 0f)
            ) { ReorderableScopeImpl(state, Orientation.Horizontal, i).content(i, item, isDragging) }
        }
    }
}
