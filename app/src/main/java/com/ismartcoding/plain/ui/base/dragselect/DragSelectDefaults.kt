package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

object DragSelectDefaults {
    val autoScrollThreshold = 100f
    
    @Composable
    fun defaultIndication() = LocalIndication.current

    @Composable
    fun defaultHapticFeedback(): HapticFeedback = LocalHapticFeedback.current
} 