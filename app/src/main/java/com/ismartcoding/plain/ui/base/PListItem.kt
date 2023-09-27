package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.palette.LocalTonalPalettes
import com.ismartcoding.plain.ui.theme.palette.onDark

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PListItem(
    modifier: Modifier = Modifier,
    enable: Boolean = true,
    title: String,
    desc: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    separatedActions: Boolean = false,
    showMore: Boolean = false,
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val tonalPalettes = LocalTonalPalettes.current
    Surface(
        modifier =
            modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .alpha(if (enable) 1f else 0.5f),
        color = Color.Unspecified,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.padding(end = 24.dp),
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                iconPainter?.let {
                    Image(
                        modifier =
                            Modifier
                                .padding(end = 24.dp)
                                .size(24.dp),
                        painter = it,
                        contentDescription = title,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                )
                desc?.let {
                    SelectionContainer {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            if (separatedActions) {
                HorizontalDivider(
                    modifier =
                        Modifier
                            .padding(start = 16.dp)
                            .size(1.dp, 32.dp),
                    color = tonalPalettes neutralVariant 80 onDark (tonalPalettes neutralVariant 30),
                )
            }

            if (value != null || action != null) {
                Box(Modifier.padding(start = 16.dp)) {
                    if (action != null) {
                        Box(Modifier.padding(end = 8.dp)) {
                            action.invoke()
                        }
                    }
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
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    modifier =
                        Modifier
                            .size(24.dp),
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
