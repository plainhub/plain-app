package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.models.MediaItem
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.components.rememberBoostFlingBehavior

@Composable
internal fun <T : MediaItem> MediaGrid(
    rows: List<T>,
    groups: List<MediaDateGroup<T>>?,
    contentType: String,
    columns: Int,
    state: LazyGridState,
    selection: DragSelectState,
    noMore: State<Boolean>,
    padding: PaddingValues,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable LazyGridItemScope.(T) -> Unit,
) {
    val flingBehavior = rememberBoostFlingBehavior(columns / 3f)
    LazyVerticalGridScrollbar(state = state) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = state,
            flingBehavior = flingBehavior,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (groups == null) {
                items(rows, key = { it.id }, contentType = { contentType }, itemContent = content)
            } else {
                for (group in groups) {
                    item(key = "header_${group.dateKey}", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        val selecting = selection.selectMode
                        val ids = if (selecting) remember(group.items) { group.items.map { it.id } } else emptyList()
                        val allSelected = ids.isNotEmpty() && ids.all(selection::isSelected)
                        MediaDateGroupHeader(
                            dateLabel = group.dateLabel,
                            allSelected = allSelected,
                            partialSelected = !allSelected && ids.any(selection::isSelected),
                            showCheckbox = selecting,
                        ) {
                            if (allSelected) selection.removeSelectedIds(ids) else selection.addSelectedIds(ids)
                        }
                    }
                    items(group.items, key = { it.id }, contentType = { contentType }, itemContent = content)
                }
            }
            item(key = "loadMore", span = { GridItemSpan(maxLineSpan) }) {
                if (!noMore.value) LaunchedEffect(Unit) { onLoadMore() }
                LoadMoreRefreshContent(noMore.value)
            }
            item(key = "bottomSpace", span = { GridItemSpan(maxLineSpan) }) { BottomSpace(padding) }
        }
    }
}
