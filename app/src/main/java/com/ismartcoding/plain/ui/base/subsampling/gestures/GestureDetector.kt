package com.ismartcoding.plain.ui.base.subsampling.gestures

import androidx.annotation.CallSuper
import androidx.compose.ui.input.pointer.PointerInputChange
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.ui.base.subsampling.gestures.GestureAction.End
import com.ismartcoding.plain.ui.base.subsampling.gestures.GestureAction.Start
import com.ismartcoding.plain.ui.base.subsampling.gestures.GestureAction.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

abstract class GestureDetector(
    val detectorType: DetectorType,
    val debug: Boolean
) {
    protected var currentGestureAnimation: GestureAnimation<*>? = null
    protected var coroutineScope: CoroutineScope? = null

    private var prevGestureAction: GestureAction? = null

    val animating: Boolean
        get() {
            if (currentGestureAnimation?.animating == true) {
                return true
            }

            return when (prevGestureAction) {
                Start -> false
                Update -> true
                End -> false
                null -> false
            }
        }

    fun cancelAnimation() {
        currentGestureAnimation?.cancel()
        currentGestureAnimation = null
    }

    @CallSuper
    open fun onGestureStarted(pointerInputChanges: List<PointerInputChange>) {
        if (debug) {
            LogCat.d("onGestureStarted() detectorType=${detectorType}")
        }

        validateGestureStart()

        coroutineScope?.cancel()
        coroutineScope = CoroutineScope(Dispatchers.Main)
    }

    @CallSuper
    open fun onGestureUpdated(pointerInputChanges: List<PointerInputChange>) {
        if (debug) {
            LogCat.d("onGestureUpdated() detectorType=${detectorType}")
        }

        validateGestureUpdate()
    }

    @CallSuper
    open fun onGestureEnded(canceled: Boolean, pointerInputChanges: List<PointerInputChange>) {
        if (debug) {
            LogCat.d("onGestureEnded() canceled=$canceled, detectorType=${detectorType}")
        }

        validateGestureEnd()

        coroutineScope?.cancel()
        coroutineScope = null
    }

    private fun validateGestureStart() {
        check(prevGestureAction == null || prevGestureAction == End) {
            "Unexpected prevGestureAction: $prevGestureAction, expected null or End"
        }
        prevGestureAction = Start
    }

    private fun validateGestureUpdate() {
        check(prevGestureAction == Start || prevGestureAction == Update) {
            "Unexpected prevGestureAction: $prevGestureAction, expected Start or Update"
        }
        prevGestureAction = Update
    }

    private fun validateGestureEnd() {
        check(prevGestureAction == Start || prevGestureAction == Update) {
            "Unexpected prevGestureAction: $prevGestureAction, expected Start or Update"
        }
        prevGestureAction = End
    }
}

internal enum class GestureAction {
    Start,
    Update,
    End
}

enum class DetectorType(val index: Int) {
    Zoom(0),
    Pan(1),
    MultiTouch(2),
    Tap(3);

    companion object {
        fun from(index: Int): DetectorType {
            return values().firstOrNull { it.index == index }
                ?: error("Unexpected index: $index")
        }
    }
}