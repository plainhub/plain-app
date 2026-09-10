package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.blue
import com.ismartcoding.plain.ui.theme.orange
import com.ismartcoding.plain.ui.theme.red
import org.jetbrains.compose.resources.painterResource

enum class AlertType {
    WARNING,
    ERROR,
    INFO,
}

/**
 * Material You tonal alert card (no accent bar): semantic container +
 * auto semantic icon + description, with optional trailing icons, an
 * action row of text buttons (PTextButton — filled buttons are too loud
 * for banners) and a custom content slot. The whole card can be clickable
 * via [onClick].
 */
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
    val accentColor = when (type) {
        AlertType.WARNING -> MaterialTheme.colorScheme.orange
        AlertType.ERROR -> MaterialTheme.colorScheme.red
        AlertType.INFO -> MaterialTheme.colorScheme.blue
    }
    val containerColor = when (type) {
        AlertType.WARNING -> accentColor.copy(alpha = 0.15f)
        AlertType.ERROR -> accentColor.copy(alpha = 0.12f)
        AlertType.INFO -> accentColor.copy(alpha = 0.12f)
    }
    val iconRes = when (type) {
        AlertType.WARNING -> Res.drawable.octagon_alert
        AlertType.ERROR -> Res.drawable.circle_alert
        AlertType.INFO -> Res.drawable.info
    }
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // Icon aligns to the first text line, not vertically centered
                // in the row (multi-line descriptions would float the icon).
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = if (trailing != null) Modifier.weight(1f) else Modifier,
                    )
                    if (trailing != null) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            trailing.invoke(this)
                        }
                    }
                }
                if (actions != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions.invoke(this)
                    }
                }
                content?.invoke(this)
            }
        }
    }
    VerticalSpace(dp = 12.dp)
}
