package com.ismartcoding.plain.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.ismartcoding.lib.helpers.FormatHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.VPackage
import com.ismartcoding.plain.ui.theme.palette.LocalTonalPalettes
import com.ismartcoding.plain.ui.theme.palette.onDark

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PackageListItem(
    item: VPackage,
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = Color.Unspecified,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp, 8.dp, 8.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier =
                Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                painter = rememberDrawablePainter(drawable = item.icon),
                contentDescription = item.name,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = item.name + " (${item.version})",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                )
                VerticalSpace(dp = 4.dp)
                Text(
                    text = item.id,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalSpace(dp = 4.dp)
                Text(
                    text = stringResource(id = LocaleHelper.getStringIdentifier("app_type_" + item.type)) + " " + FormatHelper.formatBytes(item.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VerticalSpace(dp = 4.dp)
                Text(
                    text = stringResource(id = R.string.updated_at) + "  " + item.updatedAt.formatDateTime(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
