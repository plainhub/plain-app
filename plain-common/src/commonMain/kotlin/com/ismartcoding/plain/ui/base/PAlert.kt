package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.ui.theme.warning
import com.ismartcoding.plain.ui.theme.warningContainer
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.circle_alert as ui_drawable_circle_alert
import com.ismartcoding.plain.ui.resources.circle_info as ui_drawable_circle_info
import com.ismartcoding.plain.ui.resources.octagon_alert as ui_drawable_octagon_alert

enum class AlertType {
    WARNING,
    ERROR,
    INFO,
}

@Composable
fun PAlert(
    description: String,
    type: AlertType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when (type) {
        AlertType.WARNING -> colors.warningContainer
        AlertType.ERROR -> colors.error.copy(alpha = 0.12f)
        AlertType.INFO -> colors.primary.copy(alpha = 0.12f)
    }
    val contentColor = colors.onSurface
    val iconColor = when (type) {
        AlertType.WARNING -> colors.warning
        AlertType.ERROR -> colors.error
        AlertType.INFO -> colors.primary
    }
    val iconRes = when (type) {
        AlertType.WARNING -> UiRes.drawable.ui_drawable_octagon_alert
        AlertType.ERROR -> UiRes.drawable.ui_drawable_circle_alert
        AlertType.INFO -> UiRes.drawable.ui_drawable_circle_info
    }
    val shape = MaterialTheme.shapes.medium
    val lineHeight = 24.sp

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.width(16.dp).height(with(LocalDensity.current) { lineHeight.toDp() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = lineHeight),
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                )
                if (trailing != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
                }
            }
            if (actions != null) {
                AlertSection {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        actions()
                    }
                }
            }
            if (content != null) {
                AlertSection { Column(Modifier.fillMaxWidth(), content = content) }
            }
        }
    }
}

@Composable
private fun AlertSection(content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val child = measurables.single().measure(constraints.offset(vertical = -gap))
        val top = if (child.height == 0) 0 else gap
        layout(child.width, child.height + top) { child.placeRelative(0, top) }
    }
}
