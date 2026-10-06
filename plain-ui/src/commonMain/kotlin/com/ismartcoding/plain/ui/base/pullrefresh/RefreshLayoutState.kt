package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock

@Stable
class RefreshLayoutState(
    val onRefreshListener: RefreshLayoutState.() -> Unit
) {
    val refreshContentState = mutableStateOf(RefreshContentState.Finished)
    val refreshContentOffsetState = Animatable(0f)
    val composePositionState = mutableStateOf(ComposePosition.Top)
    val refreshContentThresholdState = mutableFloatStateOf(0f)

    /**
     * 上一次**成功**刷新的时刻（epoch millis）；`null` = 本进程内还没刷成功过。
     *
     * 由 [RefreshContentState.Finished] 的那次转移打点——页面在成功时调
     * [setRefreshState]（失败走 [RefreshContentState.Failed]），所以「刷完了」
     * 等价于「有新的数据了」。**不**在刷新开始的时刻打点：那反映的是
     * 「上次尝试的时间」，用户看到会以为数据是那时候的。
     *
     * 进程内内存态，组件不碰持久化。要跨启动保留，调用方在拿到 state 后
     * [setLastRefreshTime] 灌回来，并订阅 [createLastRefreshTimeFlow] 存盘。
     */
    val lastRefreshTimeState = mutableStateOf<Long?>(null)
    private var nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }

    lateinit var coroutineScope: CoroutineScope
    var canCallRefreshListener = true

    internal val isScopeInitialized: Boolean get() = ::coroutineScope.isInitialized

    fun getRefreshContentState(): State<RefreshContentState> = refreshContentState

    fun createRefreshContentOffsetFlow(): kotlinx.coroutines.flow.Flow<Float> =
        snapshotFlow { refreshContentOffsetState.value }

    /** 上次成功刷新的时刻，`null` = 还没刷过。 */
    fun getLastRefreshTime(): State<Long?> = lastRefreshTimeState

    fun createLastRefreshTimeFlow(): kotlinx.coroutines.flow.Flow<Long?> =
        snapshotFlow { lastRefreshTimeState.value }

    /**
     * 时钟注入。默认走 [Clock.System]；测试注入假时钟才能断言「第几分钟」这种值。
     * 同一套做法见 `codeeditor/engine/EditHistory.injectClock`。
     */
    fun injectClock(clock: () -> Long) {
        nowMillis = clock
    }

    /** 由调用方恢复（例如从本地存储读回上次的时间）。 */
    fun setLastRefreshTime(millis: Long?) {
        lastRefreshTimeState.value = millis
    }

    /** 距上次成功刷新过去了多久；`null` = 没刷过。负数一律当 0（时钟被拨回去了）。 */
    fun millisSinceLastRefresh(): Long? =
        lastRefreshTimeState.value?.let { (nowMillis() - it).coerceAtLeast(0L) }

    fun getComposePositionState(): State<ComposePosition> = composePositionState

    fun getRefreshContentThreshold(): Float = refreshContentThresholdState.floatValue

    fun getRefreshContentOffset(): Float = refreshContentOffsetState.value

    internal fun offsetHoming() {
        coroutineScope.launch {
            if (abs(refreshContentOffsetState.value) >= refreshContentThresholdState.floatValue) {
                refreshContentState.value = RefreshContentState.Refreshing
                runRefreshListener()
                parkHeaderIfStillRefreshing()
            } else {
                refreshContentOffsetState.animateTo(0f)
                refreshContentState.value = RefreshContentState.Finished
            }
        }
    }

    /**
     * 触发刷新回调，并且**无论如何都把状态收尾**。
     *
     * 之前 `onRefreshListener()` 是裸调的：它一旦抛异常，外层的 `animateToThreshold()`
     * 就不执行，调用方（页面）也没机会再调 `setRefreshState(Finished)`——状态永远停在
     * [RefreshContentState.Refreshing]。而刷新中会锁住刷新头的位移，用户既看不到
     * 结束、也拉不动，一次异常就变成「这页永久卡在正在刷新」。
     *
     * 这里吞掉异常并直接收成 [RefreshContentState.Finished]：失败该由页面自己用
     * [RefreshContentState.Failed] 表达，组件只负责保证自己能回到可操作状态。
     */
    internal suspend fun runRefreshListener() {
        if (!canCallRefreshListener) {
            finishFromRefresh()
            return
        }
        try {
            onRefreshListener()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LogCat.e("刷新回调抛异常: $e")
            finishFromRefresh()
        }
    }

    /**
     * 刷新没能自己收尾时的兜底。
     *
     * **不复用 [setRefreshState]**：那条路是 `coroutineScope.launch` 出去的，
     * 而这里正待在一个刚出过异常的协程里——多一跳 `launch` 就多一次被取消的机会，
     * 状态又停在 [RefreshContentState.Refreshing]，这一页就再也拉不动了。
     * 所以直接同步置位（本函数本身是 suspend，动画可以直接 await）。
     */
    private suspend fun finishFromRefresh() {
        refreshContentState.value = RefreshContentState.Finished
        refreshContentOffsetState.animateTo(0f)
    }

    internal suspend fun animateToThreshold() {
        val composePosition = composePositionState.value
        if (composePosition == ComposePosition.Start || composePosition == ComposePosition.Top)
            refreshContentOffsetState.animateTo(refreshContentThresholdState.floatValue)
        else
            refreshContentOffsetState.animateTo(-refreshContentThresholdState.floatValue)
    }

    /**
     * 刷新回调**正常返回**后，把刷新头停在 threshold 上。
     *
     * 为什么要看状态再决定：`runRefreshListener()` 的兜底路径（回调抛异常、
     * `canCallRefreshListener = false`）会**正常返回**——它已经自己把状态收成
     * [RefreshContentState.Finished] 并把刷新头收回 0。调用方若无条件
     * `animateToThreshold()`，就会把这个已经收好的头**重新顶出去**：状态写着「已完成」，
     * 头却挂在 threshold 上，页面再也对不齐，而且拖不动。
     *
     * 这就是「卡在正在刷新」的另一半根因，和异常本身无关：只要走到兜底就必现。
     */
    internal suspend fun parkHeaderIfStillRefreshing() {
        if (refreshContentState.value == RefreshContentState.Refreshing) {
            animateToThreshold()
        }
    }

        /** 刷新成功时打点。失败路径（[RefreshContentState.Failed]）和异常兜底都不打。 */
    internal fun markRefreshed() {
        lastRefreshTimeState.value = nowMillis()
    }

    internal fun offset(refreshContentOffset: Float) {
        coroutineScope.launch {
            val targetValue = refreshContentOffsetState.value + refreshContentOffset
            if (refreshContentState.value != RefreshContentState.Dragging && targetValue != 0f) {
                refreshContentState.value = RefreshContentState.Dragging
            }
            refreshContentOffsetState.snapTo(targetValue)
        }
    }
}

@Composable
fun rememberRefreshLayoutState(onRefreshListener: RefreshLayoutState.() -> Unit) =
    remember { RefreshLayoutState(onRefreshListener) }