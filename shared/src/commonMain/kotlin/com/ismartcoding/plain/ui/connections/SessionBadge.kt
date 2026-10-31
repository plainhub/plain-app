package com.ismartcoding.plain.ui.connections

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.components.BadgeType
import com.ismartcoding.plain.ui.base.PStatusBadge
import com.ismartcoding.plain.ui.sessions.VSession

@Composable
fun SessionBadge(m: VSession, isOnline: Boolean) {
    val label = if (isOnline) stringResource(Res.string.online) else stringResource(Res.string.offline)
    PStatusBadge(
        text = label,
        type = if (isOnline) BadgeType.SUCCESS else BadgeType.NEUTRAL,
    )
}
