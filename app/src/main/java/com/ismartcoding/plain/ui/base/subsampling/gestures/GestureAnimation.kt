package com.ismartcoding.plain.ui.base.subsampling.gestures

import android.os.SystemClock
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.subsampling.ComposeSubsamplingScaleImageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class GestureAnimation<Params>(
    val debug: Boolean,
    val detectorType: DetectorType,
    val state: ComposeSubsamplingScaleImageState,
    val coroutineScope: CoroutineScope,
    val durationMs: Int,
    val animationUpdateIntervalMs: Long,
    val animationParams: () -> Params,
    val animationFunc: suspend (Params, Float, Long) -> Unit,
    val onAnimationEnd: (Boolean) -> Unit
) {
    @Volatile
    private var animationJob: Job? = null

    val animating: Boolean
        get() = animationJob != null

    fun start() {
        if (debug) {
            LogCat.d("Animation start($detectorType)")
        }

        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            val job = coroutineContext[Job]!!

            val startTime = SystemClock.elapsedRealtime()
            val params = animationParams()
            var progress = 0f
            var endedNormally = false

            try {
                while (job.isActive) {
                    if (!state.isReadyForGestures || animationJob == null) {
                        break
                    }

                    animationFunc(params, progress, durationMs.toLong())
                    delay(animationUpdateIntervalMs)

                    if (progress >= 1f) {
                        break
                    }

                    val timePassed = SystemClock.elapsedRealtime() - startTime
                    progress = timePassed.toFloat() / durationMs.toFloat()

                    if (progress > 1f) {
                        progress = 1f
                    }
                }

                endedNormally = true
            } finally {
                if (debug) {
                    LogCat.d("Animation end($detectorType) endedNormally=$endedNormally")
                }

                animationJob = null
            }
        }
    }

    fun cancel() {
        if (debug) {
            LogCat.d(
                "Animation cancel($detectorType) jobActive=${animationJob?.isActive == true}"
            )
        }

        animationJob?.cancel()
        animationJob = null

        onAnimationEnd(true)
    }
}

internal enum class GestureAnimationEasing {
    EaseOutQuad,
    EaseInOutQuad
}