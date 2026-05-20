package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.tag,
        contentDescription = stringResource(Res.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.sort,
        contentDescription = stringResource(Res.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.search,
        contentDescription = stringResource(Res.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolderKanban(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.folder_kanban,
        contentDescription = stringResource(Res.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolders(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.folder,
        contentDescription = stringResource(Res.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonCast(onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.cast,
        contentDescription = stringResource(Res.string.cast),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonInfo(contentDescription: String, onClick: () -> Unit) {
    PIconButton(
        icon = Res.drawable.info,
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
    val icon = if (isFavorite) Res.drawable.check else Res.drawable.plus
    PIconTextActionButton(
        icon = icon,
        text = stringResource(Res.string.favorites),
        click = onClick
    )
}
