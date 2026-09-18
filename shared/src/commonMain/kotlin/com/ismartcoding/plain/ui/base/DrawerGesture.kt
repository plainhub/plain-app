package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Lets a full-width horizontal row hand toward-start drags to the folder
 * drawer when the row cannot scroll any further toward its start, instead of
 * eating them. The row's scrollable consumes horizontal drags in the main
 * pass before [androidx.compose.material3.ModalNavigationDrawer]'s detector on
 * the ancestor, so swiping the drawer out from on top of the row (including
 * its blank gaps between tiles) never works without this. The interception
 * runs on the initial pass, ahead of the row's own scrollable, and only
 * claims horizontal-dominant toward-open drags while the row sits at its
 * start; vertical drags, taps and normal row scrolling are untouched.
 */
fun Modifier.drawerOpenAtRowStart(listState: LazyListState): Modifier = composed {
    val drawerState = LocalDrawerState.current ?: return@composed Modifier
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    pointerInput(drawerState, listState, isRtl) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var claiming = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                if (claiming) {
                    change.consume()
                    continue
                }
                val delta = change.positionChange()
                totalX += delta.x
                totalY += delta.y
                if (abs(totalX) > touchSlop || abs(totalY) > touchSlop) {
                    val atStart = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    val opensDrawer = if (isRtl) totalX < 0f else totalX > 0f
                    if (abs(totalX) > abs(totalY) && opensDrawer && atStart) {
                        claiming = true
                        change.consume()
                        scope.launch { drawerState.open() }
                    } else {
                        // The gesture belongs to the row or the vertical list.
                        break
                    }
                }
            }
        }
    }
}

/**
 * Invisible strip along the drawer edge that keeps swipe-to-open working when
 * the finger lands on horizontally scrolling content that is NOT at its start
 * (e.g. tab rows, a scrolled artist row). It is composed above the page
 * content, so it sees the drag first; it claims toward-open drags and opens
 * the drawer itself, mirroring the View-system DrawerLayout edge-zone
 * behavior. Vertical and away-from-drawer drags are left untouched.
 */
@Composable
internal fun BoxScope.DrawerEdgeSwipeStrip(drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = Modifier
            .align(if (isRtl) Alignment.CenterEnd else Alignment.CenterStart)
            .fillMaxHeight()
            .width(24.dp)
            .pointerInput(drawerState, isRtl) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var claiming = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (claiming) {
                            change.consume()
                            continue
                        }
                        if (change.isConsumed) break // already claimed elsewhere
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (abs(totalX) > touchSlop || abs(totalY) > touchSlop) {
                            val opensDrawer = if (isRtl) totalX < 0f else totalX > 0f
                            if (abs(totalX) > abs(totalY) && opensDrawer) {
                                claiming = true
                                change.consume()
                                scope.launch { drawerState.open() }
                            } else {
                                // Vertical scroll or a drag away from the drawer —
                                // let the content underneath handle it.
                                break
                            }
                        }
                    }
                }
            },
    )
}
