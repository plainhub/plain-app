package com.ismartcoding.plain.ui.base

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.tag,
        contentDescription = stringResource(R.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.sort,
        contentDescription = stringResource(R.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.search,
        contentDescription = stringResource(R.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolderKanban(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.folder_kanban,
        contentDescription = stringResource(R.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolders(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.folder,
        contentDescription = stringResource(R.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonCast(onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.cast,
        contentDescription = stringResource(R.string.cast),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonInfo(contentDescription: String, onClick: () -> Unit) {
    PIconButton(
        icon = R.drawable.info,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}


@Composable
fun IconTextFavoriteButton(
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    val icon = if (isFavorite) R.drawable.check else R.drawable.plus
    PIconTextActionButton(
        icon = icon,
        text = stringResource(R.string.favorites),
        click = onClick
    )
}
