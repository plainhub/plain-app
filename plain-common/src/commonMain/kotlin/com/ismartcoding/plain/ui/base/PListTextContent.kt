package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemDescription

@Composable
internal fun RowScope.PListTextContent(
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    separatedActions: Boolean = false,
    showMore: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    contentColor: Color? = null,
) {
    Column(Modifier.weight(1f)) {
        ProvideTextStyle(MaterialTheme.typography.listItemTitle().contentColored(contentColor), content = title)
        subtitle?.let {
            VerticalSpace(4.dp)
            ProvideTextStyle(MaterialTheme.typography.listItemSubtitle().contentColored(contentColor), content = it)
        }
        description?.let {
            VerticalSpace(if (subtitle == null) 4.dp else 8.dp)
            ProvideTextStyle(MaterialTheme.typography.listItemDescription().contentColored(contentColor), content = it)
        }
    }
    if (action != null || separatedActions || showMore) {
        HorizontalSpace(16.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (separatedActions) {
                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
                )
                HorizontalSpace(16.dp)
            }
            action?.invoke()
            if (showMore) PListItemChevron(contentColor, spaced = action != null)
        }
    }
}

@Composable
internal fun PListItemTitle(
    title: String,
    titleTrailing: (@Composable () -> Unit)?,
    titleSuffix: (@Composable () -> Unit)?,
    contentColor: Color?,
) {
    val style = MaterialTheme.typography.listItemTitle().contentColored(contentColor)
    when {
        titleTrailing != null -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = style, modifier = Modifier.weight(1f))
            titleTrailing()
        }
        titleSuffix != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            HorizontalSpace(4.dp)
            titleSuffix()
        }
        else -> Text(text = title, style = style)
    }
}
