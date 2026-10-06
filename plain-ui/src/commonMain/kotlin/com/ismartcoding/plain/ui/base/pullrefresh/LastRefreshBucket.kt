package com.ismartcoding.plain.ui.base.pullrefresh

/**
 * 「距上次成功刷新多久」的分桶。
 *
 * 用**相对**时间而不是 `HH:mm`：绝对时间要在 commonMain 里拿本地时区，
 * plain-ui 目前没有平台层的格式化通道，为了一个刷新头去加一条 expect/actual
 * 不划算。相对时间只需要 `now - then` 两个毫秒数，没有任何时区依赖。
 */
internal enum class LastRefreshBucket {
    /** 不到 1 分钟。 */
    JustNow,

    /** 不到 1 小时。 */
    Minutes,

    /** 不到 1 天。 */
    Hours,

    /** 1 天以上。 */
    Days,
}

/**
 * 把「过了多少毫秒」分桶，并给出该桶用的数值。
 *
 * 边界取整而不是截断：`59.9s` 显示「刚刚」而不是「0 分钟前」——
 * 「上次刷新：0 分钟前」比「刚刚」更像坏了。
 */
internal fun lastRefreshBucket(elapsedMillis: Long): Pair<LastRefreshBucket, Long> = when {
    elapsedMillis < MINUTE -> LastRefreshBucket.JustNow to 0L
    elapsedMillis < HOUR -> LastRefreshBucket.Minutes to (elapsedMillis / MINUTE)
    elapsedMillis < DAY -> LastRefreshBucket.Hours to (elapsedMillis / HOUR)
    else -> LastRefreshBucket.Days to (elapsedMillis / DAY)
}

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR