package com.ismartcoding.plain.ui.page.home
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.HomeSectionCollapsedPreference
import com.ismartcoding.plain.ui.base.PIconButton
import kotlinx.coroutines.launch

@Composable
fun HomeSectionCollapseButton(
    collapsed: Boolean,
    featureType: AppFeatureType,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    PIconButton(
        icon = if (collapsed) Res.drawable.chevron_down else Res.drawable.chevron_up,
        contentDescription = if (collapsed) stringResource(Res.string.expand_section) else stringResource(Res.string.collapse_section),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        click = {
            scope.launch {
                HomeSectionCollapsedPreference.putAsync(featureType, !collapsed)
            }
        },
    )
}
