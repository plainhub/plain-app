package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

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
        tagIcon = Res.drawable.link,
        buttonIcon = Res.drawable.square_arrow_out_up_right,
    )
}