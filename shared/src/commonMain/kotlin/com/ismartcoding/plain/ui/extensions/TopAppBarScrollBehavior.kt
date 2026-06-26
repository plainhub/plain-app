package com.ismartcoding.plain.ui.extensions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
fun TopAppBarScrollBehavior.reset() {
    state.heightOffset = 0f
}