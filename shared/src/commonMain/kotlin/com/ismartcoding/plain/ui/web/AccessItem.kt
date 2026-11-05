package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.manage_files
import com.ismartcoding.plain.platform.permissions.AccessIssue
import com.ismartcoding.plain.platform.permissions.accessIssues
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

internal val managedFileAccessFeatures = setOf(
    AccessFeature.IMAGES, AccessFeature.VIDEOS, AccessFeature.AUDIO,
    AccessFeature.DOCS, AccessFeature.FILES,
)

internal data class AccessItem(
    val icon: DrawableResource?,
    val features: Set<AccessFeature>,
    internal val issues: List<AccessIssue> = features.accessIssues(),
) {
    fun isEnabled(enabled: Set<AccessFeature>): Boolean = enabled.containsAll(features)

    @Composable
    fun getText(): String = if (features == managedFileAccessFeatures) stringResource(Res.string.manage_files)
        else features.single().getText()
}
