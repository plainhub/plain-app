package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomSpace(paddingValues: PaddingValues? = null, modifier: Modifier = Modifier) {
    if (paddingValues != null) {
        VerticalSpace(dp = 40.dp + paddingValues.calculateBottomPadding(), modifier = modifier)
    } else {
        VerticalSpace(dp = 40.dp, modifier = modifier)
    }
}
