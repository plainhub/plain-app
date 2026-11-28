package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun PListItem(
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    contentColor: Color? = null,
    title: String,
    subtitle: String = "",
    value: String? = null,
    icon: DrawableResource? = null,
    start: (@Composable RowScope.() -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    titleSuffix: (@Composable () -> Unit)? = null,
    separatedActions: Boolean = false,
    showMore: Boolean = false,
    verticalPadding: Dp = Spacing.sm,
    action: (@Composable () -> Unit)? = null,
) {
    val hasValue = !value.isNullOrEmpty()
    PListItemRow(
        modifier, dimmed, if (subtitle.isNotEmpty()) 72.dp else 56.dp, icon, start, contentColor,
        verticalPadding = verticalPadding,
    ) {
        if (hasValue) {
            PListKeyValueContent(title, value.orEmpty(), subtitle, showMore, contentColor)
        } else {
            PListTextContent(
                title = { PListItemTitle(title, titleTrailing, titleSuffix, contentColor) },
                subtitle = if (subtitle.isNotEmpty()) ({ PListItemSubtitle(subtitle, contentColor) }) else null,
                separatedActions = separatedActions,
                showMore = showMore,
                action = action,
                contentColor = contentColor,
            )
        }
    }
}

@Composable
fun PListItem(
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    icon: DrawableResource? = null,
    start: (@Composable RowScope.() -> Unit)? = null,
    statusColor: Color? = null,
    minHeight: Dp = if (subtitle != null || description != null) 72.dp else 56.dp,
    action: (@Composable () -> Unit)? = null,
) {
    PListItemRow(modifier, dimmed, minHeight, icon, start, contentColor = null, statusColor = statusColor) {
        PListTextContent(title, subtitle, description, action = action)
    }
}

@Composable
private fun PListItemRow(
    modifier: Modifier,
    dimmed: Boolean,
    minHeight: Dp,
    icon: DrawableResource?,
    start: (@Composable RowScope.() -> Unit)?,
    contentColor: Color?,
    statusColor: Color? = null,
    verticalPadding: Dp = Spacing.sm,
    content: @Composable RowScope.() -> Unit,
) {
    val status = if (statusColor == null) Modifier else Modifier.drawBehind {
        val width = 4.dp.toPx()
        drawRect(
            color = statusColor,
            topLeft = Offset(if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width, 0f),
            size = Size(width, size.height),
        )
    }
    Row(
        modifier = modifier
            .alpha(if (dimmed) 0.5f else 1f)
            .then(status)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = minHeight)
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (start != null) {
            Row(verticalAlignment = Alignment.CenterVertically, content = start)
            HorizontalSpace(16.dp)
        } else if (icon != null) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(icon),
                colorFilter = ColorFilter.tint(contentColor ?: MaterialTheme.colorScheme.primary),
                contentDescription = null,
            )
            HorizontalSpace(16.dp)
        }
        content()
    }
}
