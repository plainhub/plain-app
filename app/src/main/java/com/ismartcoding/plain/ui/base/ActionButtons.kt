package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun ActionButtonMore(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.Outlined.MoreVert,
        contentDescription = stringResource(R.string.more),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonMoreWithMenu(content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    PIconButton(
        icon = Icons.Outlined.MoreVert,
        contentDescription = stringResource(R.string.more),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = {
            isMenuOpen = true
        },
    )
    PDropdownMenu(
        expanded = isMenuOpen,
        onDismissRequest = { isMenuOpen = false }
    ) {
        content {
            isMenuOpen = false
        }
    }
}

@Composable
fun ActionButtonAdd(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.Outlined.Add,
        contentDescription = stringResource(R.string.add),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonSettings(
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    PIconButton(
        icon = Icons.Outlined.Settings,
        contentDescription = stringResource(R.string.settings),
        tint = MaterialTheme.colorScheme.onSurface,
        showBadge = showBadge,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonSelect(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.Outlined.Checklist,
        contentDescription = stringResource(R.string.select),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.AutoMirrored.Outlined.Label,
        contentDescription = stringResource(R.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.AutoMirrored.Outlined.Sort,
        contentDescription = stringResource(R.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}


@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = Icons.Outlined.Search,
        contentDescription = stringResource(R.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    )
}