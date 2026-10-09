package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun TopSpace(modifier: Modifier = Modifier) {
    VerticalSpace(dp = PlainTheme.PAGE_TOP_MARGIN, modifier = modifier)
}
