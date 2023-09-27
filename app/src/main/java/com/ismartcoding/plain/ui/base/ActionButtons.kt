package com.ismartcoding.plain.ui.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun ActionButtonMore(onClick: () -> Unit) {
    PIconButton(
        imageVector = Icons.Outlined.MoreVert,
        contentDescription = stringResource(R.string.more),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonSettings(onClick: () -> Unit) {
    PIconButton(
        imageVector = Icons.Outlined.Settings,
        contentDescription = stringResource(R.string.settings),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}
