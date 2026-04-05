package com.ismartcoding.plain.ui.page.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.components.MediaFolderGridItem
import com.ismartcoding.plain.ui.components.MediaFolderListItem

@Composable
internal fun MediaFolderListContent(
    listState: LazyListState,
    totalBucket: DMediaBucket?,
    items: List<DMediaBucket>,
    selectedBucketId: String,
    onSelect: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item { TopSpace() }
        if (totalBucket != null) {
            item {
                MediaFolderListItem(
                    folder = totalBucket,
                    isSelected = selectedBucketId.isEmpty(),
                    onClick = { onSelect("") }
                )
            }
        }
        items(items = items, key = { it.id }) { folder ->
            MediaFolderListItem(
                folder = folder,
                isSelected = selectedBucketId == folder.id,
                onClick = { onSelect(folder.id) }
            )
        }
        item { BottomSpace() }
    }
}

@Composable
internal fun MediaFolderGridContent(
    gridState: LazyGridState,
    totalBucket: DMediaBucket?,
    items: List<DMediaBucket>,
    selectedBucketId: String,
    onSelect: (String) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (totalBucket != null) {
            item(
                key = "all",
                contentType = { "bucket" },
                span = { GridItemSpan(1) }
            ) {
                MediaFolderGridItem(
                    m = totalBucket,
                    isSelected = selectedBucketId.isEmpty(),
                    onClick = { onSelect("") }
                )
            }
        }
        items(
            items,
            key = { it.id },
            contentType = { "bucket" },
            span = { GridItemSpan(1) }
        ) { m ->
            MediaFolderGridItem(
                m = m,
                isSelected = selectedBucketId == m.id,
                onClick = { onSelect(m.id) }
            )
        }
        item(
            span = { GridItemSpan(maxLineSpan) },
            key = "bottomSpace"
        ) {
            BottomSpace()
        }
    }
}
