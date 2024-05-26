package com.ismartcoding.plain.ui.base.fastscroll

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.ismartcoding.plain.ui.base.fastscroll.controller.StateController
import com.ismartcoding.plain.ui.base.fastscroll.foundation.ScrollbarLayoutSettings
import com.ismartcoding.plain.ui.base.fastscroll.foundation.VerticalScrollbarLayout

@Composable
internal fun <IndicatorValue> ElementScrollbar(
    stateController: StateController<IndicatorValue>,
    modifier: Modifier,
    settings: ScrollbarSettings,
    indicatorContent: (@Composable (indicatorValue: IndicatorValue, isThumbSelected: Boolean) -> Unit)?,
) {
    val view = LocalView.current
    val layoutSettings = remember(settings) {
        ScrollbarLayoutSettings(
            scrollbarPadding = settings.scrollbarPadding,
            selectionActionable = settings.selectionActionable,
            hideEasingAnimation = settings.hideEasingAnimation,
            hideDisplacement = settings.hideDisplacement,
            hideDelayMillis = settings.hideDelayMillis,
            durationAnimationMillis = settings.durationAnimationMillis,
        )
    }

    BoxWithConstraints(modifier) {
        val maxLengthPixels = constraints.maxHeight.toFloat()

        VerticalScrollbarLayout(
            thumbOffsetNormalized = stateController.thumbOffsetNormalized.value,
            thumbIsInAction = stateController.thumbIsInAction.value,
            thumbIsSelected = stateController.isSelected.value,
            settings = layoutSettings,
            indicator = indicatorContent?.let {
                { it(stateController.indicatorValue(), stateController.isSelected.value) }
            },
            draggableModifier = Modifier.draggable(
                state = rememberDraggableState { deltaPixel ->
                    stateController.onDraggableState(
                        deltaPixels = deltaPixel,
                        maxLengthPixels = maxLengthPixels
                    )
                },
                orientation = Orientation.Vertical,
                enabled = settings.selectionMode != ScrollbarSelectionMode.Disabled,
                startDragImmediately = true,
                onDragStarted = { offsetPixel ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    stateController.onDragStarted(
                        offsetPixels = offsetPixel.y,
                        maxLengthPixels = maxLengthPixels
                    )
                },
                onDragStopped = {
                    stateController.onDragStopped()
                }
            )
        )
    }
}
