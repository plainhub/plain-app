package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun TopSpace() {
    VerticalSpace(dp = PlainTheme.PAGE_TOP_MARGIN)
}

@Composable
fun BottomSpace(paddingValues: PaddingValues? = null) {
    if (paddingValues != null) {
        VerticalSpace(dp = 40.dp + paddingValues.calculateBottomPadding())
    } else {
        VerticalSpace(dp = 40.dp)
    }
}
