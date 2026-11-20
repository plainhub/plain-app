package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.Spacing
import com.ismartcoding.plain.ui.theme.warning
import com.ismartcoding.plain.ui.theme.warningContainer
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
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
    var firstLineCenter by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        AlertLayout(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier)
                .padding(start = Spacing.lg, end = Spacing.sm),
            header = {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp).alignBy { it.measuredHeight / 2 },
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).alignBy { firstLineCenter },
                        color = contentColor,
                        onTextLayout = { firstLineCenter = ((it.getLineTop(0) + it.getLineBottom(0)) / 2).roundToInt() },
                    )
                    if (trailing != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
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
        val inset = Spacing.sm.roundToPx()
        val compact = Spacing.xs.roundToPx()
        val loose = constraints.copy(minHeight = 0)
        val headingPlaceable = heading.single().measure(loose.offset(vertical = -inset - compact))
        val afterHeading = inset + headingPlaceable.height
        val buttonPlaceable = buttons.singleOrNull()?.measure(loose.offset(vertical = -afterHeading - compact))
        val buttonHeight = buttonPlaceable?.height ?: 0
        val afterButtons = afterHeading + buttonHeight
        val detailPlaceable = body.singleOrNull()?.measure(loose.offset(vertical = -afterButtons - inset * 2))
        val hasDetails = (detailPlaceable?.height ?: 0) > 0
        val detailStart = afterButtons + if (hasDetails) inset else 0
        val bottom = if (buttonHeight > 0 && !hasDetails) compact else inset
        val width = constraints.constrainWidth(
            maxOf(headingPlaceable.width, buttonPlaceable?.width ?: 0, detailPlaceable?.width ?: 0),
        )
        val height = constraints.constrainHeight(detailStart + (detailPlaceable?.height ?: 0) + bottom)
        layout(width, height) {
            headingPlaceable.placeRelative(0, inset)
            buttonPlaceable?.placeRelative(0, afterHeading)
            detailPlaceable?.placeRelative(0, detailStart)
        }
    }
}
