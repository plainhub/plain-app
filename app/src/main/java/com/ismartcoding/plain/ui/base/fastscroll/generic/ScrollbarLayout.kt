package com.ismartcoding.plain.ui.base.fastscroll.generic

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.base.fastscroll.foundation.HorizontalScrollbarLayout
import com.ismartcoding.plain.ui.base.fastscroll.foundation.ScrollbarLayoutSettings
import com.ismartcoding.plain.ui.base.fastscroll.foundation.VerticalScrollbarLayout

@Composable
internal fun ScrollbarLayout(
    orientation: Orientation,
    thumbSizeNormalized: Float,
    thumbOffsetNormalized: Float,
    thumbIsInAction: Boolean,
    thumbIsSelected: Boolean,
    settings: ScrollbarLayoutSettings,
    draggableModifier: Modifier,
    indicator: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (orientation) {
        Orientation.Vertical -> VerticalScrollbarLayout(
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            thumbIsSelected = thumbIsSelected,
            settings = settings,
            draggableModifier = draggableModifier,
            indicator = indicator,
            modifier = modifier,
        )

        Orientation.Horizontal -> HorizontalScrollbarLayout(
            thumbSizeNormalized = thumbSizeNormalized,
            thumbOffsetNormalized = thumbOffsetNormalized,
            thumbIsInAction = thumbIsInAction,
            thumbIsSelected = thumbIsSelected,
            settings = settings,
            draggableModifier = draggableModifier,
            indicator = indicator,
            modifier = modifier,
        )
    }
}
