package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PModalBottomSheet(
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier.defaultMinSize(minHeight = 320.dp),
    // Full-height sheets extend to the very top of the screen behind the
    // status bar and open directly expanded: content must apply its own
    // inset padding. Top corners round over while the sheet is dragged (or
    // animates) away from the screen top; flat again when fully expanded.
    fullHeight: Boolean = false,
    sheetGesturesEnabled: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = fullHeight),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val topCornerShape by remember(density, sheetState) {
        derivedStateOf {
            // requireOffset throws before the first layout pass (NaN anchor).
            val offset = runCatching { sheetState.requireOffset() }.getOrDefault(0f)
            with(density) {
                val corner = ((offset / 96.dp.toPx()).coerceIn(0f, 1f) * 24.dp.toPx()).toDp()
                RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 0.dp, bottomEnd = 0.dp)
            }
        }
    }
    ModalBottomSheet(
        modifier = if (fullHeight) modifier else modifier.statusBarsPadding(),
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = if (fullHeight) topCornerShape else BottomSheetDefaults.ExpandedShape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        contentWindowInsets = { if (fullHeight) WindowInsets(0) else BottomSheetDefaults.windowInsets },
        content = content
    )
}
