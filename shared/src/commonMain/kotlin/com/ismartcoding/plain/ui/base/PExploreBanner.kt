package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import com.ismartcoding.plain.i18n.link

@Composable
fun PExploreBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PFeatureBanner(
        modifier = modifier,
        tag = stringResource(Res.string.official),
        title = stringResource(Res.string.official_website_title),
        description = stringResource(Res.string.official_website_desc),
        buttonText = "plainapp.app",
        onClick = onClick,
        tagIcon = UiRes.drawable.ui_drawable_link,
        buttonIcon = UiRes.drawable.ui_drawable_square_arrow_out_up_right,
    )
}