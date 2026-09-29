package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.info as ui_drawable_info
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import com.ismartcoding.plain.ui.resources.search as ui_drawable_search
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import com.ismartcoding.plain.ui.resources.tag as ui_drawable_tag
import com.ismartcoding.plain.i18n.sort
import com.ismartcoding.plain.i18n.folder
import com.ismartcoding.plain.i18n.search

@Composable
fun ActionButtonTags(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_tag,
        contentDescription = stringResource(Res.string.tags),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSort(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_sort,
        contentDescription = stringResource(Res.string.sort),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonSearch(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_search,
        contentDescription = stringResource(Res.string.search),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonFolders(onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_folder,
        contentDescription = stringResource(Res.string.folders),
        tint = MaterialTheme.colorScheme.onSurface,
        click = onClick,
    )
}

@Composable
fun ActionButtonInfo(contentDescription: String, onClick: () -> Unit) {
    PIconButton(
        icon = UiRes.drawable.ui_drawable_info,
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
    val icon = if (isFavorite) UiRes.drawable.ui_drawable_check else UiRes.drawable.ui_drawable_plus
    PIconTextActionButton(
        icon = icon,
        text = stringResource(Res.string.favorites),
        click = onClick
    )
}
