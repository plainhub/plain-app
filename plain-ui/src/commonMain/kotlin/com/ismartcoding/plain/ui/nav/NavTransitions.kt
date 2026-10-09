package com.ismartcoding.plain.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlin.reflect.KClass

private const val PUSH_DURATION_MS = 300
private const val PUSH_FADE_DURATION_MS = 150
private const val PRESENT_DURATION_MS = 300

/**
 * 页面切换转场，挂到 NavHost 的四个 transition 参数上：
 * `enterTransition = { navEnterTransition() }` 等等。
 *
 * 默认一律水平推入/滑出并带视差（下面的页面只移动 1/3）。把「从底部升起的全屏页」路由类型
 * 放进 [presentedRoutes]，这些页面改用垂直升起/落下，且被覆盖时下方页面保持静止。
 */
private fun NavDestination.isPresented(presentedRoutes: Set<KClass<out Any>>): Boolean =
    presentedRoutes.any { hasRoute(it) }

/** 进入：模态页面从底部升起（渐显），普通页面从右侧推入 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(
    presentedRoutes: Set<KClass<out Any>> = emptySet(),
): EnterTransition =
    if (targetState.destination.isPresented(presentedRoutes)) {
        slideInVertically(tween(PRESENT_DURATION_MS, easing = LinearOutSlowInEasing)) { it } +
            fadeIn(tween(PRESENT_DURATION_MS, easing = LinearOutSlowInEasing))
    } else {
        slideInHorizontally(tween(PUSH_DURATION_MS, easing = LinearOutSlowInEasing)) { it } +
            fadeIn(tween(PUSH_FADE_DURATION_MS, 50, easing = LinearOutSlowInEasing))
    }

/** 退出：被模态页面覆盖时保持静止，否则视差滑向左侧 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(
    presentedRoutes: Set<KClass<out Any>> = emptySet(),
): ExitTransition =
    if (targetState.destination.isPresented(presentedRoutes)) ExitTransition.None
    else slideOutHorizontally(tween(PUSH_DURATION_MS, easing = FastOutLinearInEasing)) { -it / 3 } +
        fadeOut(tween(PUSH_FADE_DURATION_MS, easing = FastOutLinearInEasing))

/** pop 后重新进入：模态页面落下时下方保持静止，否则视差滑回原位 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopEnterTransition(
    presentedRoutes: Set<KClass<out Any>> = emptySet(),
): EnterTransition =
    if (initialState.destination.isPresented(presentedRoutes)) EnterTransition.None
    else slideInHorizontally(tween(PUSH_DURATION_MS, easing = LinearOutSlowInEasing)) { -it / 3 } +
        fadeIn(tween(PUSH_FADE_DURATION_MS, 50, easing = LinearOutSlowInEasing))

/** pop 退出：模态页面向底部落下（渐隐），普通页面滑向右侧 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopExitTransition(
    presentedRoutes: Set<KClass<out Any>> = emptySet(),
): ExitTransition =
    if (initialState.destination.isPresented(presentedRoutes)) {
        slideOutVertically(tween(PRESENT_DURATION_MS, easing = FastOutLinearInEasing)) { it } +
            fadeOut(tween(PRESENT_DURATION_MS, easing = FastOutLinearInEasing))
    } else {
        slideOutHorizontally(tween(PUSH_DURATION_MS, easing = FastOutLinearInEasing)) { it } +
            fadeOut(tween(PUSH_FADE_DURATION_MS, easing = FastOutLinearInEasing))
    }