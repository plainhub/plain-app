package com.ismartcoding.plain.ui.page.home

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.ismartcoding.plain.enums.AppFeatureType

data class FeatureItem(
    val type: AppFeatureType,
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    val click: () -> Unit,
) {
    companion object
}
