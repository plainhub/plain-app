package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun NavigationBackIcon(onClick: () -> Unit = {}) {
    PIconButton(
        icon = R.drawable.arrow_left,
        contentDescription = stringResource(R.string.back),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}

@Composable
fun NavigationCloseIcon(onClick: () -> Unit = {}) {
    PIconButton(
        icon = R.drawable.x,
        contentDescription = stringResource(R.string.close),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}

