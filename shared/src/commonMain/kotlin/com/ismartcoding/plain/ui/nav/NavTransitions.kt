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

private const val PUSH_DURATION = 300
private const val PUSH_FADE_DURATION = 150
private const val PRESENT_DURATION = 300

/**
 * 以模态方式呈现的路由集合：这些页面从底部垂直升起/落下，其余页面左右水平推入/推出。
 * 拥有文件夹抽屉的媒体详情页（Images/Audio/Videos/Docs）与 [Routing.Files] 保持一致的转场。
 *
 * 层级关系由 NavHost 管理：push 的页面盖在栈上方，pop 时退出的页面绘制在最上层，
 * 因此 PRESENT 页面升降时下方页面保持静止（[EnterTransition.None]/[ExitTransition.None]）。
 */
private val PRESENTED_ROUTES: Set<KClass<out Any>> = setOf(
    Routing.Files::class,
    Routing.ChatText::class,
    Routing.Images::class,
    Routing.Audio::class,
    Routing.Videos::class,
    Routing.Docs::class,
    Routing.Notes::class,
    Routing.FeedEntries::class,
    Routing.SoundMeter::class,
    Routing.PomodoroTimer::class,
    Routing.ImageEditor::class,
    Routing.Files::class,
)

/** 当前目的地是否为模态呈现 */
private fun NavDestination.isPresented(): Boolean = PRESENTED_ROUTES.any { hasRoute(it) }

/** 目标页面为模态呈现（垂直升起） */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.presenting() = targetState.destination.isPresented()

/** 初始页面为模态呈现（垂直落下） */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.dismissing() = initialState.destination.isPresented()

/** 进入：模态页面从底部升起（渐显），普通页面从右侧推入 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(): EnterTransition =
    if (presenting()) {
        slideInVertically(tween(PRESENT_DURATION, easing = LinearOutSlowInEasing)) { it } +
            fadeIn(tween(PRESENT_DURATION, easing = LinearOutSlowInEasing))
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

/** pop 退出：模态页面向底部落下（渐隐），普通页面滑向右侧 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopExitTransition(): ExitTransition =
    if (dismissing()) {
        slideOutVertically(tween(PRESENT_DURATION, easing = FastOutLinearInEasing)) { it } +
            fadeOut(tween(PRESENT_DURATION, easing = FastOutLinearInEasing))
    } else {
        slideOutHorizontally(tween(PUSH_DURATION, easing = FastOutLinearInEasing)) { it } +
            fadeOut(tween(PUSH_FADE_DURATION, easing = FastOutLinearInEasing))
    }
