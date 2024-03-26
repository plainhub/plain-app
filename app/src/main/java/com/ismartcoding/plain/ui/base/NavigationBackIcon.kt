package com.ismartcoding.plain.ui.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun NavigationBackIcon(onClick: () -> Unit = {}) {
    PIconButton(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(R.string.back),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}

@Composable
fun NavigationCloseIcon(onClick: () -> Unit = {}) {
    PIconButton(
        imageVector = Icons.Rounded.Close,
        contentDescription = stringResource(R.string.back),
        tint = MaterialTheme.colorScheme.onSurface,
    ) {
        onClick()
    }
}

