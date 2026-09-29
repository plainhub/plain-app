package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.ellipsis_vertical as ui_drawable_ellipsis_vertical
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.menu as ui_drawable_menu
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import com.ismartcoding.plain.ui.resources.refresh_ccw as ui_drawable_refresh_ccw
import com.ismartcoding.plain.ui.resources.scan_qr_code as ui_drawable_scan_qr_code
import com.ismartcoding.plain.ui.resources.settings as ui_drawable_settings
import com.ismartcoding.plain.i18n.settings

@Composable
fun ActionButtons(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 5, content = content
    )
}

@Composable
fun ActionButtonDrawer(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_menu, contentDescription = stringResource(Res.string.more),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}

@Composable
fun ActionButtonMore(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_ellipsis_vertical, contentDescription = stringResource(Res.string.more),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}

@Composable
fun ActionButtonMoreWithMenu(content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    PIconButton(icon = UiRes.drawable.ui_drawable_ellipsis_vertical, contentDescription = stringResource(Res.string.more),
        tint = MaterialTheme.colorScheme.onSurface, click = { isMenuOpen = true })
    PDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) { content { isMenuOpen = false } }
}

@Composable
fun ActionButtonAddWithMenu(content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }
    PIconButton(icon = UiRes.drawable.ui_drawable_plus, contentDescription = stringResource(Res.string.add),
        tint = MaterialTheme.colorScheme.onSurface, click = { isMenuOpen = true })
    PDropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) { content { isMenuOpen = false } }
}

@Composable
fun ActionButtonAdd(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_plus, contentDescription = stringResource(Res.string.add),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}

@Composable
fun ActionButtonScan(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_scan_qr_code, contentDescription = stringResource(Res.string.scan_qrcode),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}

@Composable
fun ActionButtonSettings(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_settings, contentDescription = stringResource(Res.string.settings),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}

@Composable
fun ActionButtonRefresh(onClick: () -> Unit, loading: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "refresh_rotation"
    )
    PIconButton(icon = UiRes.drawable.ui_drawable_refresh_ccw, contentDescription = stringResource(Res.string.refresh),
        tint = if (loading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = if (loading) Modifier.rotate(rotation) else Modifier, click = onClick)
}

@Composable
fun ActionButtonSettings(showBadge: Boolean = false, onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_settings, contentDescription = stringResource(Res.string.settings),
        tint = MaterialTheme.colorScheme.onSurface, showBadge = showBadge, click = onClick)
}

@Composable
fun ActionButtonSelect(onClick: () -> Unit) {
    PIconButton(icon = UiRes.drawable.ui_drawable_list_checks, contentDescription = stringResource(Res.string.select),
        tint = MaterialTheme.colorScheme.onSurface, click = onClick)
}
