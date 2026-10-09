package com.ismartcoding.plain.ui.base

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.listItemValue
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_right as ui_drawable_chevron_right


@Composable
fun PListItem(
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    title: String,
    subtitle: String = "",
    value: String? = null,
    icon: DrawableResource? = null,
    // ListItem supplies the outer padding and the gap after this slot.
    start: (@Composable RowScope.() -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    titleSuffix: (@Composable () -> Unit)? = null,
    separatedActions: Boolean = false,
    showMore: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    ListItem(
        modifier = modifier.alpha(if (enable) 1f else 0.5f),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            when {
                titleTrailing != null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.listItemTitle(),
                            modifier = Modifier.weight(1f),
                        )
                        titleTrailing()
                    }
                }
                titleSuffix != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.listItemTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        HorizontalSpace(4.dp)
                        titleSuffix()
                    }
                }
                else -> Text(
                    text = title,
                    style = MaterialTheme.typography.listItemTitle(),
                )
            }
        },
        supportingContent = if (subtitle.isNotEmpty()) {
            {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.listItemSubtitle(),
                )
            }
        } else null,
        leadingContent = when {
            start != null -> {
                { Row(verticalAlignment = Alignment.CenterVertically, content = start) }
            }
            icon != null -> {
                {
                    Image(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(icon),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        contentDescription = title,
                    )
                }
            }
            else -> null
        },
        trailingContent = if (value != null || action != null || showMore || separatedActions) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (separatedActions) {
                        VerticalDivider(
                            modifier = Modifier.height(24.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
                        )
                        HorizontalSpace(16.dp)
                    }
                    action?.invoke()
                    value?.let {
                        SelectionContainer {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.listItemValue(),
                            )
                        }
                    }
                    if (showMore) {
                        if (value != null || action != null) HorizontalSpace(8.dp)
                        Icon(
                            painter = painterResource(UiRes.drawable.ui_drawable_chevron_right),
                            modifier = Modifier.size(16.dp),
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else null,
    )
}
