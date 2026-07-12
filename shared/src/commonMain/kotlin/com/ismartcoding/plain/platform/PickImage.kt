package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
expect fun PickImageEffect(imageUrl: MutableState<String>)
