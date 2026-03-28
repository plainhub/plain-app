package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed

internal val HorizontalTextPadding = 16.dp

@Composable
fun PScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 16.dp,
    tabs: @Composable () -> Unit,
) {
    ScrollableTabRowImp(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        edgePadding = edgePadding,
        tabs = tabs,
        scrollState = rememberScrollState(),
    )
}

@Composable
private fun ScrollableTabRowImp(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 16.dp,
    tabs: @Composable () -> Unit,
    scrollState: ScrollState,
) {
    Surface(modifier = modifier, color = Color.Transparent) {
        val coroutineScope = rememberCoroutineScope()
        val scrollableTabData = remember(scrollState, coroutineScope) {
            ScrollableTabData(scrollState = scrollState, coroutineScope = coroutineScope)
        }
        SubcomposeLayout(
            Modifier.fillMaxWidth().wrapContentSize(align = Alignment.CenterStart)
                .horizontalScroll(scrollState).selectableGroup().clipToBounds()
        ) { constraints ->
            val padding = edgePadding.roundToPx()
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)

            val layoutHeight = tabMeasurables.fastFold(initial = 0) { curr, measurable ->
                maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
            }

            val tabPlaceables = mutableListOf<Placeable>()
            val tabContentWidths = mutableListOf<Dp>()
            tabMeasurables.fastForEach {
                val placeable = it.measure(constraints)
                var contentWidth = minOf(it.maxIntrinsicWidth(placeable.height), placeable.width).toDp()
                contentWidth -= HorizontalTextPadding * 2
                tabPlaceables.add(placeable)
                tabContentWidths.add(contentWidth)
            }

            val layoutWidth = tabPlaceables.fastFold(initial = padding * 2) { curr, measurable ->
                curr + measurable.width
            }

            layout(layoutWidth, layoutHeight) {
                val tabPositions = mutableListOf<TabPosition>()
                var left = padding
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(left, 0)
                    tabPositions.add(TabPosition(left = left.toDp(), width = placeable.width.toDp(), contentWidth = tabContentWidths[index]))
                    left += placeable.width
                }
                scrollableTabData.onLaidOut(
                    density = this@SubcomposeLayout, edgeOffset = padding,
                    tabPositions = tabPositions, selectedTab = selectedTabIndex,
                )
            }
        }
    }
}
