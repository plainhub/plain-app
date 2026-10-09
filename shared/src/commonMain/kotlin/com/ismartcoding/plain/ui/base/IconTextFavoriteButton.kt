package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import org.jetbrains.compose.resources.stringResource

@Composable
fun IconTextFavoriteButton(
    isFavorite: Boolean = false,
    onClick: () -> Unit
) {
    val icon = if (isFavorite) UiRes.drawable.ui_drawable_check else UiRes.drawable.ui_drawable_plus
    PIconTextActionButton(
        icon = icon,
        text = stringResource(Res.string.favorites),
        onClick = onClick
    )
}
