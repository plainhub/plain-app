package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BottomSpace() {
    VerticalSpace(dp = 40.dp)
}

@Composable
fun HorizontalSpace(dp: Dp) {
    Spacer(Modifier.width(dp))
}

@Composable
fun VerticalSpace(dp: Dp) {
    Spacer(Modifier.height(dp))
}
