package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.Placeholder
import com.bumptech.glide.integration.compose.Transition

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PGlideImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    failure: Placeholder? = null,
    transition: Transition.Factory? = null,
) {
    if (model.endsWith(".svg", true)) {
        AsyncImage(
            model = model, contentDescription = contentDescription, modifier = modifier,
            alignment = alignment, contentScale = contentScale,
        )
    } else {
        GlideImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            alignment = alignment,
            contentScale = contentScale,
            failure = failure,
            transition = transition,
        )
    }
}