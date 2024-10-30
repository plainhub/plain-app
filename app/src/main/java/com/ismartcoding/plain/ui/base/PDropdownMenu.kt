package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier.defaultMinSize(minWidth = 160.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(MaterialTheme.colorScheme.inverseOnSurface),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 0.dp,
        ),
        enabled = enabled,
    )
}

@Composable
fun PDropdownMenuItemCreateFolder(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.create_folder)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.folder_plus),
            contentDescription = stringResource(id = R.string.create_folder)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemCreateFile(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.create_file)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.file_plus),
            contentDescription = stringResource(id = R.string.create_file)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemCast(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.cast_mode)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.cast),
            contentDescription = stringResource(id = R.string.cast_mode)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemSort(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.sort)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.sort),
            contentDescription = stringResource(id = R.string.sort)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemTags(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.tags)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.tags),
            contentDescription = stringResource(id = R.string.tags)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemSettings(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.settings),
            contentDescription = stringResource(id = R.string.settings)
        )
    }, onClick = onClick)
}

@Composable
fun PDropdownMenuItemCellsPerRow(onClick: () -> Unit) {
    PDropdownMenuItem(text = { Text(stringResource(R.string.cells_per_row)) }, leadingIcon = {
        Icon(
            painter = painterResource(R.drawable.grid_3x3),
            contentDescription = stringResource(id = R.string.cells_per_row)
        )
    }, onClick = onClick)
}