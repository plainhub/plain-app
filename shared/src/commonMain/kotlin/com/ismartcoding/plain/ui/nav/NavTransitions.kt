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

private const val PUSH_DURATION = 300
private const val PUSH_FADE_DURATION = 150
private const val PRESENT_DURATION = 300

/**
 * 导航转场由页面左上角图标决定：
 * PUSH —— 有返回箭头的页面，水平方向推入/推出；
 * PRESENT —— 有关闭按钮的页面（模态呈现），垂直方向升起/落下，进出互为反向。
 *
 * 层级关系由 NavHost 管理：push 的页面盖在栈上方，pop 时退出的页面绘制在最上层，
 * 因此 PRESENT 页面升降时下方页面保持静止（[EnterTransition.None]/[ExitTransition.None]）。
 */
private fun NavDestination.isPresented() = hasRoute<Routing.Files>() || hasRoute<Routing.ChatText>()

/** 目标页面为模态呈现（垂直升起） */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.presenting() = targetState.destination.isPresented()

/** 初始页面为模态呈现（垂直落下） */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.dismissing() = initialState.destination.isPresented()

/** 进入：模态页面从底部升起，普通页面从右侧推入 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(): EnterTransition =
    if (presenting()) {
        slideInVertically(tween(PRESENT_DURATION, easing = LinearOutSlowInEasing)) { it }
    } else {
        slideInHorizontally(tween(PUSH_DURATION, easing = LinearOutSlowInEasing)) { it } +
            fadeIn(tween(PUSH_FADE_DURATION, 50, easing = LinearOutSlowInEasing))
    }

/** 退出：被模态页面覆盖时保持静止，否则视差滑向左侧 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(): ExitTransition =
    if (presenting()) ExitTransition.None
    else slideOutHorizontally(tween(PUSH_DURATION, easing = FastOutLinearInEasing)) { -it / 3 } +
        fadeOut(tween(PUSH_FADE_DURATION, easing = FastOutLinearInEasing))

/** pop 后重新进入：模态页面落下时下方保持静止，否则视差滑回原位 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopEnterTransition(): EnterTransition =
    if (dismissing()) EnterTransition.None
    else slideInHorizontally(tween(PUSH_DURATION, easing = LinearOutSlowInEasing)) { -it / 3 } +
        fadeIn(tween(PUSH_FADE_DURATION, 50, easing = LinearOutSlowInEasing))

/** pop 退出：模态页面向底部落下，普通页面滑向右侧 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopExitTransition(): ExitTransition =
    if (dismissing()) {
        slideOutVertically(tween(PRESENT_DURATION, easing = FastOutLinearInEasing)) { it }
    } else {
        slideOutHorizontally(tween(PUSH_DURATION, easing = FastOutLinearInEasing)) { it } +
            fadeOut(tween(PUSH_FADE_DURATION, easing = FastOutLinearInEasing))
    }
