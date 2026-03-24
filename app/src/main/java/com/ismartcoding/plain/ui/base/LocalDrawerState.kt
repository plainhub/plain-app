package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalDrawerState = compositionLocalOf<DrawerState?> { null }

/**
 * When pager is on the first page, forward rightward drags to the drawer.
 * Leftward drags (to next tab) are consumed normally by the pager.
 */
class DrawerNestedScrollConnection(
    private val pagerState: PagerState,
    private val drawerState: DrawerState,
    private val scope: CoroutineScope,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Only intercept when on first page and dragging right (opening drawer)
        if (source == NestedScrollSource.UserInput &&
            pagerState.currentPage == 0 &&
            !pagerState.isScrollInProgress &&
            available.x > 0 &&
            drawerState.currentValue == DrawerValue.Closed
        ) {
            scope.launch { drawerState.open() }
            return available
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return Velocity.Zero
    }
}
