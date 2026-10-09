package com.ismartcoding.plain.ui.nav

import kotlin.reflect.KClass

/**
 * 以模态方式呈现的路由集合：从底部垂直升起/落下，其余页面水平推入。
 * 转场实现见 plain-ui 的 `navEnterTransition` 等函数。
 */
internal val PRESENTED_ROUTES: Set<KClass<out Any>> = setOf(
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
    Routing.Apps::class,
    Routing.SharedFolder::class,
)