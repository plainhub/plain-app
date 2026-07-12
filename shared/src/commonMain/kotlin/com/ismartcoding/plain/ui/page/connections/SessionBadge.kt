package com.ismartcoding.plain.ui.page.connections

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.enums.BadgeType
import com.ismartcoding.plain.ui.base.PStatusBadge
import com.ismartcoding.plain.ui.models.VSession

@Composable
fun SessionBadge(m: VSession, isOnline: Boolean) {
    val label = if (m.isCustom) stringResource(Res.string.custom_token) else if (isOnline) stringResource(Res.string.online) else stringResource(Res.string.offline)
    PStatusBadge(
        text = label,
        type = if (!m.isCustom && isOnline) BadgeType.SUCCESS else BadgeType.NEUTRAL,
    )
}