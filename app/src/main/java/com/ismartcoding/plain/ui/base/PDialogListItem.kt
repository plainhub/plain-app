package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.palette.onDark

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PDialogListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String? = null,
    value: String? = null,
    icon: Any? = null,
    separatedActions: Boolean = false,
    showMore: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(0.dp, 8.dp, 8.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            if (icon is ImageVector) {
                Icon(
                    modifier = Modifier.padding(end = 16.dp),
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (icon is Painter) {
                Image(
                    modifier =
                    Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    painter = icon,
                    contentDescription = title,
                )
            } else if (icon is String) {
                AsyncImage(
                    model = icon,
                    contentDescription = title,
                    modifier = Modifier
                        .size(24.dp),
                )
                HorizontalSpace(dp = 16.dp)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            )
            desc?.let {
                VerticalSpace(dp = 8.dp)
                SelectionContainer {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
        if (separatedActions) {
            VerticalDivider(
                modifier =
                Modifier
                    .height(32.dp)
                    .padding(start = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (value != null || action != null) {
            Box(Modifier.padding(start = 16.dp)) {
                action?.invoke()
                value?.let {
                    Box(Modifier.padding(end = if (showMore) 0.dp else 8.dp, top = 8.dp, bottom = 8.dp)) {
                        SelectionContainer {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                            )
                        }
                    }
                }
            }
        }

        if (showMore) {
            Icon(
                painter = painterResource(id = R.drawable.chevron_right),
                modifier =
                Modifier
                    .size(24.dp),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
