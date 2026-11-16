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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.ui.theme.Spacing
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
    val leadingSize = 20.dp
    val firstLineHeight = maxOf(leadingSize, with(LocalDensity.current) { lineHeight.toDp() })

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        AlertLayout(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
                .padding(horizontal = Spacing.lg),
            header = {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Box(
                        modifier = Modifier.width(leadingSize).height(firstLineHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(leadingSize))
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = lineHeight),
                        modifier = Modifier.weight(1f),
                        color = contentColor,
                    )
                    if (trailing != null) {
                        Row(Modifier.heightIn(min = firstLineHeight), verticalAlignment = Alignment.CenterVertically, content = trailing)
                    }
                }
            },
            actions = {
                if (actions != null) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        content = actions,
                    )
                }
            },
            details = {
                if (content != null) Column(Modifier.fillMaxWidth(), content = content)
            },
        )
    }
}

@Composable
private fun AlertLayout(
    modifier: Modifier,
    header: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    details: @Composable () -> Unit,
) {
    Layout(contents = listOf(header, actions, details), modifier = modifier) { (heading, buttons, body), constraints ->
        val inset = (Spacing.sm + Spacing.xs).roundToPx()
        val compact = Spacing.xs.roundToPx()
        val detailGap = Spacing.sm.roundToPx()
        val loose = constraints.copy(minHeight = 0)
        val headingPlaceable = heading.single().measure(loose.offset(vertical = -inset - compact))
        val afterHeading = inset + headingPlaceable.height
        val buttonPlaceable = buttons.singleOrNull()?.measure(loose.offset(vertical = -afterHeading - compact * 2))
        val hasButtons = (buttonPlaceable?.height ?: 0) > 0
        val buttonStart = afterHeading + if (hasButtons) compact else 0
        val afterButtons = buttonStart + (buttonPlaceable?.height ?: 0)
        val detailPlaceable = body.singleOrNull()?.measure(loose.offset(vertical = -afterButtons - detailGap - inset))
        val hasDetails = (detailPlaceable?.height ?: 0) > 0
        val detailStart = afterButtons + if (hasDetails) detailGap else 0
        val bottom = if (hasButtons && !hasDetails) compact else inset
        val width = constraints.constrainWidth(
            maxOf(headingPlaceable.width, buttonPlaceable?.width ?: 0, detailPlaceable?.width ?: 0),
        )
        val height = constraints.constrainHeight(detailStart + (detailPlaceable?.height ?: 0) + bottom)
        layout(width, height) {
            headingPlaceable.placeRelative(0, inset)
            buttonPlaceable?.placeRelative(0, buttonStart)
            detailPlaceable?.placeRelative(0, detailStart)
        }
    }
}
