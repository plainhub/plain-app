package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun VerticalSpace(dp: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier.height(dp))
}
